package com.v7lthronyx.scamynx.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.GlowOrb
import com.v7lthronyx.scamynx.common.designsystem.ParticleBackground
import com.v7lthronyx.scamynx.common.designsystem.ScamynxGradients
import com.v7lthronyx.scamynx.common.designsystem.ScamynxPrimary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxSecondary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxSignalGreen
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTertiary80
import com.v7lthronyx.scamynx.common.designsystem.spacing
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val iconColor: Color,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Shield,
        iconColor = ScamynxPrimary80,
        titleRes = R.string.onboarding_page1_title,
        descriptionRes = R.string.onboarding_page1_description,
    ),
    OnboardingPage(
        icon = Icons.Filled.Language,
        iconColor = ScamynxSecondary80,
        titleRes = R.string.onboarding_page2_title,
        descriptionRes = R.string.onboarding_page2_description,
    ),
    OnboardingPage(
        icon = Icons.Filled.QrCodeScanner,
        iconColor = ScamynxTertiary80,
        titleRes = R.string.onboarding_page3_title,
        descriptionRes = R.string.onboarding_page3_description,
    ),
    OnboardingPage(
        icon = Icons.Filled.Security,
        iconColor = ScamynxSignalGreen,
        titleRes = R.string.onboarding_page4_title,
        descriptionRes = R.string.onboarding_page4_description,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScamynxGradients.backdrop()),
    ) {
        
        ParticleBackground(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.4f),
            particleCount = 25,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.screenPadding),
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.md),
                contentAlignment = Alignment.CenterEnd,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = stringResource(id = R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    isCurrentPage = pagerState.currentPage == page,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.lg),
                ) {
                    onboardingPages.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateFloatAsState(
                            targetValue = if (isSelected) 24f else 8f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            label = "indicatorWidth",
                        )

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) {
                                        ScamynxPrimary80
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                if (isLastPage) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScamynxSignalGreen,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.onboarding_get_started),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_next),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isCurrentPage: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.85f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "pageScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.5f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "pageAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .alpha(alpha)
            .padding(horizontal = MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            
            GlowOrb(
                modifier = Modifier.size(160.dp),
                color = page.iconColor,
                size = 80.dp,
                glowRadius = 40.dp,
            )

            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = page.iconColor.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = page.iconColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))

        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

        Text(
            text = stringResource(id = page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
            lineHeight = 26.sp,
        )
    }
}
