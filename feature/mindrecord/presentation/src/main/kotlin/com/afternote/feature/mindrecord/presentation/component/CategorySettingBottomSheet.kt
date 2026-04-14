package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingBottomSheet(
    categories: List<CategoryUiModel>,
    onDismiss: () -> Unit,
    onBackClick: () -> Unit,
    onAddCategory: () -> Unit,
    onMenuClick: (CategoryUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDDDDD)),
            )
        },
    ) {
        CategorySettingContent(
            categories = categories,
            onBackClick = onBackClick,
            onAddCategory = onAddCategory,
            onMenuClick = onMenuClick,
        )
    }
}

// ── Content ────────────────────────────────────────────────────────────────

@Composable
fun CategorySettingContent(
    categories: List<CategoryUiModel>,
    onBackClick: () -> Unit,
    onAddCategory: () -> Unit,
    onMenuClick: (CategoryUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
    ) {
        // 헤더
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 뒤로가기
            Icon(
                painter = painterResource(R.drawable.core_ui_arrow_left),
                contentDescription = "뒤로가기",
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(24.dp)
                        .clickable { onBackClick() },
                tint = AfternoteDesign.colors.gray9,
            )
            Text(
                text = "카테고리 설정하기",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 새 카테고리 만들기
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAddCategory() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.core_ui_add), // plus 아이콘
                contentDescription = null,
                tint = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "새 카테고리 만들기",
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
            )
        }

        HorizontalDivider(color = Color(0xFF000000).copy(alpha = 0.07f))

        // 카테고리 목록
        categories.forEach { category ->
            CategoryItem(
                category = category,
                onMenuClick = { onMenuClick(category) },
            )
            HorizontalDivider(color = Color(0xFF000000).copy(alpha = 0.07f))
        }
    }
}

// ── Item ───────────────────────────────────────────────────────────────────

@Composable
fun CategoryItem(
    category: CategoryUiModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 색상 원
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(category.color),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = category.name,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.weight(1f),
        )
        // 더보기 메뉴
        Icon(
            painter = painterResource(R.drawable.core_ui_vertical), // ⋮ 아이콘
            contentDescription = "더보기",
            tint = AfternoteDesign.colors.gray5,
            modifier =
                Modifier
                    .size(20.dp)
                    .clickable { onMenuClick() },
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun CategorySettingContentPreview() {
    AfternoteTheme {
        val sampleCategories =
            listOf(
                CategoryUiModel(id = "1", name = "나의 가치관", color = Color(0xFF1A1A1A)),
                CategoryUiModel(id = "2", name = "오늘 떠올린 생각", color = Color(0xFFFFB3A7)),
                CategoryUiModel(id = "3", name = "인생을 되돌아 보며", color = Color(0xFFA8C8E8)),
            )

        CategorySettingContent(
            categories = sampleCategories,
            onBackClick = {},
            onAddCategory = {},
            onMenuClick = {},
        )
    }
}
