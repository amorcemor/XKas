package com.mibi.xkas.ui.debt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibi.xkas.utils.formatRupiah

@Composable
fun DebtSummaryCard(
    totalCustomerOwes: Double, // Hutang Pelanggan ke Saya
    totalBusinessOwes: Double, // Hutang Saya ke Pelanggan
    totalDebtors: Int,
    totalDebts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Summary Card - mirip desain gambar
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2E3B4E) // Dark blue-gray seperti gambar
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header - mirip "RINGKASAN PIUTANG"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RINGKASAN",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Net Balance Section - dihilangkan sesuai permintaan awal
                // (tidak ada saldo bersih)

                // Grid 2x2 untuk 4 item - mirip layout di gambar
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Hutang Pelanggan & Hutang Saya
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hutang Pelanggan Card - mirip "Hutang Pelanggan" di gambar
                        FinancialCard(
                            amount = totalCustomerOwes,
                            title = "Hutang Pelanggan",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFFFF5252), // Red
                            modifier = Modifier.weight(1f)
                        )

                        // Hutang Saya Card - mirip "Hutang Kita" di gambar
                        FinancialCard(
                            amount = totalBusinessOwes,
                            title = "Hutang Saya",
                            icon = Icons.Default.TrendingDown,
                            color = Color(0xFF4CAF50), // Green
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Total Debitur & Total Catatan - mirip "2 Kontak" dan "9 Catatan"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            value = totalDebtors.toString(),
                            label = "Kontak",
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            value = totalDebts.toString(),
                            label = "Catatan",
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialCard(
    amount: Double,
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3A4A5C) // Sedikit lebih terang dari background utama
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon dengan warna sesuai
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            // Amount - mirip "Rp3.000" dan "Rp8.000" di gambar
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            // Title - mirip "Hutang Pelanggan" dan "Hutang Kita"
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3A4A5C) // Sama dengan FinancialCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )

            // Value dan Label - mirip "2 Kontak" dan "9 Catatan"
            Text(
                text = "$value $label",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DebtSummaryCardPreview() {
    MaterialTheme {
        DebtSummaryCard(
            totalCustomerOwes = 15750000.0,
            totalBusinessOwes = 5250000.0,
            totalDebtors = 12,
            totalDebts = 25
        )
    }
}