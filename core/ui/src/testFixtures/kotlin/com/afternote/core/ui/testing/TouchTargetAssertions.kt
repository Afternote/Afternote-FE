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
        val isNamedTrailingActionInClickableListRow =
            name.isNotBlank() &&
                role != null &&
                clickAncestors.singleOrNull()?.let { node.isTrailingAccessoryOf(it) } == true

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
            hasClickAncestor =
                clickAncestors.isNotEmpty() &&
                    !isNamedTextFieldTrailingButton &&
                    !isNamedTrailingActionInClickableListRow,
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

/**
 * 이 노드가 [container] 의 **끝단 보조 액션**인가 (#1669).
 *
 * 「눌러서 이동하는 목록 행 + 그 끝의 오버플로 메뉴」는 Android 목록의 정본 형태이고
 * (Material3 `ListItem` 의 `trailingContent`), TalkBack 은 unmerged tree 에서 행과 버튼을
 * 각각 별개 노드로 짚어 준다. 그래서 중첩 그 자체는 결함이 아니다.
 *
 * 결함이 되는 것은 **어느 쪽을 누르는지 모호할 때**다. 그 모호함을 세 축으로 가른다 —
 * 행 모양(가로가 세로보다 길다), 보조 크기(행 너비의 1/4 이하), 끝단 위치(중심이 뒷절반).
 * 이름과 Role 은 호출부에서 따로 본다. 셋 다 만족해야 «행의 끝에 달린 작은 액션» 이고,
 * 반반으로 갈린 두 클릭 영역이나 정사각 상자 안의 정사각 버튼은 여기 들어오지 못한다.
 */
private fun SemanticsNode.isTrailingAccessoryOf(container: SemanticsNode): Boolean {
    val own = boundsInRoot
    val outer = container.boundsInRoot
    if (outer.width <= 0f || outer.height <= 0f) return false

    val containerIsRow = outer.width > outer.height
    val isAccessorySized = own.width <= outer.width / MAX_TRAILING_ACCESSORY_WIDTH_RATIO
    val sitsInTrailingHalf = own.center.x >= outer.center.x

    return containerIsRow && isAccessorySized && sitsInTrailingHalf
}

/**
 * 끝단 보조가 차지해도 되는 행 너비의 역수. 1/4 을 넘으면 «행에 달린 작은 액션» 이 아니라
 * 행을 나눠 갖는 두 번째 영역이라, 어느 쪽을 눌렀는지 모호해진다.
 */
private const val MAX_TRAILING_ACCESSORY_WIDTH_RATIO = 4

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
