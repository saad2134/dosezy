package com.example.dosezy.ui.subscreens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Star
import com.example.dosezy.utils.AppSignature
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun AboutScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

    val btcAddress = "bc1q9r3qll0ya5wznvmdqz7wgdn7xy5dwmy5g23l6q"
    val ethAddress = "0xB95fc8BF67E70b5c9126A46d2B48FDE1E9868FA5"
    val solAddress = "9JcRUwDARh2mruwZAsPeUkDs6ME4DVtWXUWTiVZMxDvW"
    val ltcAddress = "LZfi2pYxnx23DLSdigeg6Hy8BDVbTDcppE"
    val zecAddress = "t1SNFk17ixbrx5oaaRQiHTt8ryQvgwnQ2AG"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = stringResource(R.string.about_title),
            showBackButton = true,
            actions = {}
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header / App Branding
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF2084E4).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.loader_icon),
                            contentDescription = "Dosezy Logo",
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Dosezy",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.about_version_build, versionName ?: "2.5.2", versionCode),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.about_tagline),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Why Dosezy? (Core Pillars & Features)
            Text(
                text = stringResource(R.string.about_features_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AboutFeatureItem(
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF10B981),
                    title = stringResource(R.string.about_feat_offline_title),
                    description = stringResource(R.string.about_feat_offline_desc)
                )

                AboutFeatureItem(
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFF0284C7),
                    title = stringResource(R.string.about_feat_smart_alarms_title),
                    description = stringResource(R.string.about_feat_smart_alarms_desc)
                )

                AboutFeatureItem(
                    icon = Icons.Default.People,
                    iconTint = Color(0xFF8B5CF6),
                    title = stringResource(R.string.about_feat_family_title),
                    description = stringResource(R.string.about_feat_family_desc)
                )

                AboutFeatureItem(
                    icon = Icons.Default.Accessibility,
                    iconTint = Color(0xFFF59E0B),
                    title = stringResource(R.string.about_feat_accessible_title),
                    description = stringResource(R.string.about_feat_accessible_desc)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About the Developer Section
            Text(
                text = stringResource(R.string.about_developer_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2084E4).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF2084E4),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppSignature.authorName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.about_developer_role),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.about_developer_bio),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AboutLinkCard(
                        icon = Icons.Outlined.Star,
                        iconTint = Color(0xFF2084E4),
                        title = stringResource(R.string.about_developer_github),
                        description = "@${AppSignature.authorHandle}",
                        onClick = {
                            openUrl(context, AppSignature.githubProfileUrl)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Open Source & Community
            Text(
                text = stringResource(R.string.about_community_open_source),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AboutLinkCard(
                icon = Icons.Outlined.Star,
                iconTint = Color(0xFFEAB308),
                title = stringResource(R.string.about_star_github),
                description = stringResource(R.string.about_star_github_desc),
                onClick = {
                    openUrl(context, "https://github.com/saad2134/dosezy")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Support Development Section
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
                    openUrl(context, "https://buymeacoffee.com/saad1inc")
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
                    openUrl(context, "https://github.com/sponsors/saad2134")
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutLinkCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Outlined.ArrowOutward,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
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
        } catch (e: Exception) {
            // No registered wallet app handler on device; address is copied to clipboard
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.err_cannot_open_link), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AboutFeatureItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
