package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.VividIndigo
import com.example.ui.components.VividViolet
import com.example.ui.components.VividRose
import com.example.viewmodel.FinanceActivityViewModel
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: FinanceActivityViewModel,
    onFinished: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    val pages = remember(appLang) {
        if (appLang == "id") {
            listOf(
                OnboardingPageData(
                    title = "Selamat Datang di FinAct AI",
                    description = "Aplikasi keuangan dan aktivitas harian masa depan Anda, bertenaga kecerdasan buatan Gemini AI. Bicara secara alami seperti mengobrol dengan asisten pribadi!",
                    icon = Icons.Default.Star,
                    color = VividViolet
                ),
                OnboardingPageData(
                    title = "Perencana Skenario 'What-If'",
                    description = "Simulasikan keputusan keuangan masa depan Anda seperti mengambil cicilan, meningkatkan pendapatan, atau berhemat, dan lihat dampaknya langsung ke tujuan tabungan Anda.",
                    icon = Icons.Default.PlayArrow,
                    color = VividIndigo
                ),
                OnboardingPageData(
                    title = "Transaksi Berulang Otomatis",
                    description = "Kelola pengeluaran langganan, tagihan bulanan, atau tabungan berkala Anda secara otomatis. Sistem akan mencatatnya tepat waktu tanpa repot.",
                    icon = Icons.Default.Refresh,
                    color = VividRose
                ),
                OnboardingPageData(
                    title = "Kategori & Atribut Dinamis",
                    description = "Atur kategori khusus yang Anda inginkan. Form input pintar kami juga akan beradaptasi secara dinamis mengikuti setiap poin penting baru yang ditemukan oleh AI!",
                    icon = Icons.Default.AccountCircle,
                    color = Color(0xFF00E5FF)
                )
            )
        } else {
            listOf(
                OnboardingPageData(
                    title = "Welcome to FinAct AI",
                    description = "Your next-gen finance and activity companion powered by Gemini AI. Chat naturally with your assistant to track money and habits!",
                    icon = Icons.Default.Star,
                    color = VividViolet
                ),
                OnboardingPageData(
                    title = "Interactive 'What-If' Scenario",
                    description = "Simulate big life and financial decisions like taking loans, job promotions, or aggressive saving, instantly projecting long-term impacts.",
                    icon = Icons.Default.PlayArrow,
                    color = VividIndigo
                ),
                OnboardingPageData(
                    title = "Automated Recurring Loggers",
                    description = "Easily schedule repeating transactions (daily, weekly, monthly, yearly). Our local automation logs them on target dates automatically.",
                    icon = Icons.Default.Refresh,
                    color = VividRose
                ),
                OnboardingPageData(
                    title = "Dynamic Adaptation",
                    description = "Customize categories and watch manual entry forms evolve instantly as Gemini identifies new attributes in your conversations!",
                    icon = Icons.Default.AccountCircle,
                    color = Color(0xFF00E5FF)
                )
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        GlassmorphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FinAct AI",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pages.size - 1)
                            }
                        }
                    ) {
                        Text(
                            text = if (appLang == "id") "Lewati" else "Skip",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Icon Card
                    GlassCard(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = page.color,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = page.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Description
                    Text(
                        text = page.description,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Footer / Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                                .background(
                                    color = if (isSelected) pages[index].color else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Next / Get Started button
                val isLastPage = pagerState.currentPage == pages.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            viewModel.completeOnboarding()
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(VividViolet, VividIndigo)
                                )
                            )
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLastPage) {
                                    if (appLang == "id") "Mulai Sekarang" else "Get Started"
                                } else {
                                    if (appLang == "id") "Lanjut" else "Next"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
