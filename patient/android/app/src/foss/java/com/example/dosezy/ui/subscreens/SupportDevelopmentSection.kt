package com.example.dosezy.ui.subscreens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R

/**
 * Support Development section for F-Droid and GitHub sideload distribution.
 *
 * Provides voluntary project support options including Buy Me a Coffee, GitHub Sponsors,
 * and multi-cryptocurrency donation addresses (BTC, ETH, SOL, LTC, ZEC).
 */
@Composable
fun SupportDevelopmentSection() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val btcAddress = "bc1q9r3qll0ya5wznvmdqz7wgdn7xy5dwmy5g23l6q"
    val ethAddress = "0xB95fc8BF67E70b5c9126A46d2B48FDE1E9868FA5"
    val solAddress = "9JcRUwDARh2mruwZAsPeUkDs6ME4DVtWXUWTiVZMxDvW"
    val ltcAddress = "LZfi2pYxnx23DLSdigeg6Hy8BDVbTDcppE"
    val zecAddress = "t1SNFk17ixbrx5oaaRQiHTt8ryQvgwnQ2AG"

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))

        // Support Development Title
        Text(
            text = stringResource(R.string.about_support_dev_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = stringResource(R.string.about_support_dev_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
            lineHeight = 18.sp
        )

        // Buy Me a Coffee Card
        AboutLinkCard(
            icon = Icons.Outlined.Coffee,
            iconTint = Color(0xFFFF813F),
            title = stringResource(R.string.about_buymeacoffee),
            description = stringResource(R.string.about_buymeacoffee_desc),
            onClick = {
                openDonationUrl(context, "https://buymeacoffee.com/saad1inc")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // GitHub Sponsors Card
        AboutLinkCard(
            icon = Icons.Outlined.Favorite,
            iconTint = Color(0xFFEA4AAA),
            title = stringResource(R.string.about_github_sponsors),
            description = stringResource(R.string.about_github_sponsors_desc),
            onClick = {
                openDonationUrl(context, "https://github.com/sponsors/saad2134")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3rd Option: Cryptocurrency Donations (Collapsible)
        var isCryptoExpanded by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isCryptoExpanded = !isCryptoExpanded },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF7931A).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CurrencyBitcoin,
                            contentDescription = null,
                            tint = Color(0xFFF7931A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.about_crypto_donations),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "BTC, ETH, SOL, LTC, ZEC",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = if (isCryptoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isCryptoExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isCryptoExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bitcoin Card
                        CryptoAddressCard(
                            currencyName = stringResource(R.string.crypto_bitcoin),
                            address = btcAddress,
                            badgeColor = Color(0xFFF7931A),
                            onClick = {
                                handleCryptoClick(context, clipboardManager, context.getString(R.string.crypto_name_btc), btcAddress, "bitcoin")
                            }
                        )

                        // Ethereum Card
                        CryptoAddressCard(
                            currencyName = stringResource(R.string.crypto_ethereum),
                            address = ethAddress,
                            badgeColor = Color(0xFF627EEA),
                            onClick = {
                                handleCryptoClick(context, clipboardManager, context.getString(R.string.crypto_name_eth), ethAddress, "ethereum")
                            }
                        )

                        // Solana Card
                        CryptoAddressCard(
                            currencyName = stringResource(R.string.crypto_solana),
                            address = solAddress,
                            badgeColor = Color(0xFF14F195),
                            onClick = {
                                handleCryptoClick(context, clipboardManager, context.getString(R.string.crypto_name_sol), solAddress, "solana")
                            }
                        )

                        // Litecoin Card
                        CryptoAddressCard(
                            currencyName = stringResource(R.string.crypto_litecoin),
                            address = ltcAddress,
                            badgeColor = Color(0xFF345D9D),
                            onClick = {
                                handleCryptoClick(context, clipboardManager, context.getString(R.string.crypto_name_ltc), ltcAddress, "litecoin")
                            }
                        )

                        // Zcash Card
                        CryptoAddressCard(
                            currencyName = stringResource(R.string.crypto_zcash),
                            address = zecAddress,
                            badgeColor = Color(0xFFF4B728),
                            onClick = {
                                handleCryptoClick(context, clipboardManager, context.getString(R.string.crypto_name_zec), zecAddress, "zcash")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoAddressCard(
    currencyName: String,
    address: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currencyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.about_tap_to_copy),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = address,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun handleCryptoClick(
    context: Context,
    clipboardManager: ClipboardManager,
    currencyDisplayName: String,
    address: String,
    scheme: String?
) {
    clipboardManager.setText(AnnotatedString(address))
    Toast.makeText(
        context,
        context.getString(R.string.about_copied_to_clipboard, currencyDisplayName),
        Toast.LENGTH_SHORT
    ).show()

    if (!scheme.isNullOrBlank()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme:$address"))
            context.startActivity(intent)
        } catch (_: Exception) {
            // No registered wallet app handler on device; address is copied to clipboard
        }
    }
}

private fun openDonationUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.err_cannot_open_link), Toast.LENGTH_SHORT).show()
    }
}

