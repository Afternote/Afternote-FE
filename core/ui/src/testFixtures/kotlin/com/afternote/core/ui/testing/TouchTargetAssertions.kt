package com.afternote.core.ui.testing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp

/** Android 권장 최소 터치 타깃인 48dp를 앱 공통 기준으로 사용한다. */
val MinimumTouchTargetSize: Dp = 48.dp

/** unmerged semantics tree에서 찾은 활성 클릭 타깃 하나의 접근성 진단 정보. */
data class EnabledClickTarget(
    val index: Int,
    /** 시스템 최소 터치 타깃 확장과 ancestor clipping이 모두 반영된 실제 hit-test 영역. */
    val touchBounds: DpRect,
    /** 시스템 hit 확장 전의 clipped layout 영역. 크기 실패 판정에는 쓰지 않는다. */
    val layoutBounds: DpRect,
    /** ContentDescription 또는 화면에 표시되는 Text로 얻은 접근 가능한 이름. */
    val name: String,
    val testTag: String?,
    val role: Role?,
    val stateDescription: String?,
    val toggleableState: ToggleableState?,
    val selected: Boolean?,
    val isEditableText: Boolean,
    val hasClickAncestor: Boolean,
) {
    val width: Dp get() = touchBounds.right - touchBounds.left
    val height: Dp get() = touchBounds.bottom - touchBounds.top
    val layoutWidth: Dp get() = layoutBounds.right - layoutBounds.left
    val layoutHeight: Dp get() = layoutBounds.bottom - layoutBounds.top

    fun isSmallerThan(minimumSize: Dp): Boolean = width < minimumSize || height < minimumSize

    internal fun lacksRole(): Boolean = role == null && !isEditableText

    internal fun lacksRequiredState(): Boolean =
        when (role) {
            Role.Checkbox, Role.Switch -> toggleableState == null && stateDescription.isNullOrBlank()
            Role.RadioButton, Role.Tab -> selected == null && stateDescription.isNullOrBlank()
            else -> false
        }

    internal fun diagnostic(): String =
        buildString {
            append("#$index touch=$width×$height layout=$layoutWidth×$layoutHeight")
            val label = name.ifBlank { testTag.orEmpty() }
            if (label.isNotBlank()) append(" name=\"").append(label).append('"')
            role?.let { append(" role=").append(it) }
            stateDescription?.let { append(" state=\"").append(it).append('"') }
            toggleableState?.let { append(" toggle=").append(it) }
            selected?.let { append(" selected=").append(it) }
            if (hasClickAncestor) append(" nested-click=true")
        }
}

/**
 * 활성 OnClick semantics만 unmerged tree에서 수집한다.
 *
 * [SemanticsNode.touchBoundsInRoot]는 Foundation이 실제 hit-test에 적용한 48dp 확장과
 * ancestor clipping을 반영한다. px 좌표이므로 각 노드의 density로 [DpRect]로 변환한다.
 */
fun SemanticsNodeInteractionsProvider.scanEnabledClickTargets(): List<EnabledClickTarget> {
    val interactions = onAllNodes(hasClickAction() and isEnabled(), useUnmergedTree = true)
    val nodes = interactions.fetchSemanticsNodes()

    return nodes.mapIndexedNotNull { index, node ->
        if (!node.hasEnabledClickAction()) return@mapIndexedNotNull null

        val name = node.accessibleName()
        val role = node.config.getOrNull(SemanticsProperties.Role)
        val clickAncestors =
            generateSequence(node.parent) { it.parent }
                .filter { it.hasEnabledClickAction() }
                .toList()
        val isNamedTextFieldTrailingButton =
            clickAncestors.isNotEmpty() &&
                role == Role.Button &&
                name.isNotBlank() &&
                clickAncestors.all { it.isEditableFocusTarget() }

        EnabledClickTarget(
            index = index,
            touchBounds = node.touchBoundsInRoot.toDpRect(node),
            layoutBounds = node.boundsInRoot.toDpRect(node),
            name = name,
            testTag = node.config.getOrNull(SemanticsProperties.TestTag),
            role = role,
            stateDescription = node.config.getOrNull(SemanticsProperties.StateDescription),
            toggleableState = node.config.getOrNull(SemanticsProperties.ToggleableState),
            selected = node.config.getOrNull(SemanticsProperties.Selected),
            isEditableText = node.isEditableFocusTarget(),
            hasClickAncestor = clickAncestors.isNotEmpty() && !isNamedTextFieldTrailingButton,
        )
    }
}

/** 활성 클릭 타깃의 hit bounds, 이름, 역할, 상태 및 중첩 클릭 계약을 단언한다. */
fun SemanticsNodeInteractionsProvider.assertAccessibleClickTargets(minimumSize: Dp = MinimumTouchTargetSize) {
    val targets = scanEnabledClickTargets()
    if (targets.isEmpty()) {
        throw AssertionError("활성 클릭 semantics가 하나도 없습니다")
    }

    val tooSmall = targets.filter { it.isSmallerThan(minimumSize) }
    val unnamed = targets.filter { it.name.isBlank() }
    val missingRole = targets.filter { it.lacksRole() }
    val missingState = targets.filter { it.lacksRequiredState() }
    val nested = targets.filter { it.hasClickAncestor }
    if (
        tooSmall.isEmpty() &&
        unnamed.isEmpty() &&
        missingRole.isEmpty() &&
        missingState.isEmpty() &&
        nested.isEmpty()
    ) {
        return
    }

    throw AssertionError(
        buildString {
            appendLine("활성 클릭 semantics 계약 위반 (최소 $minimumSize×$minimumSize)")
            appendViolations("48dp 미달", tooSmall)
            appendViolations("접근 가능한 이름 누락", unnamed)
            appendViolations("Role 누락", missingRole)
            appendViolations("선택 상태 누락", missingState)
            appendViolations("중첩 클릭", nested)
        }.trimEnd(),
    )
}

private fun StringBuilder.appendViolations(
    title: String,
    targets: List<EnabledClickTarget>,
) {
    if (targets.isEmpty()) return
    appendLine("$title:")
    targets.forEach { appendLine("- ${it.diagnostic()}") }
}

private fun SemanticsNode.hasEnabledClickAction(): Boolean =
    config.getOrNull(SemanticsActions.OnClick)?.action != null &&
        config.getOrNull(SemanticsProperties.Disabled) == null

private fun SemanticsNode.isEditableFocusTarget(): Boolean =
    config.getOrNull(SemanticsProperties.EditableText) != null &&
        config.getOrNull(SemanticsActions.RequestFocus)?.action != null

private fun SemanticsNode.accessibleName(): String {
    val ownDescriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    if (ownDescriptions.any { it.isNotBlank() }) {
        return ownDescriptions.filter { it.isNotBlank() }.joinToString()
    }

    val ownTexts =
        config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() }
    if (ownTexts.isNotEmpty()) return ownTexts.joinToString()

    return children
        .map { it.accessibleName() }
        .filter { it.isNotBlank() }
        .joinToString()
}

private fun Rect.toDpRect(node: SemanticsNode): DpRect {
    val pxBounds = this
    return with(node.layoutInfo.density) {
        DpRect(
            left = pxBounds.left.toDp(),
            top = pxBounds.top.toDp(),
            right = pxBounds.right.toDp(),
            bottom = pxBounds.bottom.toDp(),
        )
    }
}
