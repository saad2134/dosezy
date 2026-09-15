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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                        text = "Version $versionName (Build $versionCode)",
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

            // Open Source & Community
            Text(
                text = "Community & Open Source",
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

            Spacer(modifier = Modifier.height(18.dp))

            // Crypto Donations Header
            Text(
                text = stringResource(R.string.about_crypto_donations),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Bitcoin Card
            CryptoAddressCard(
                currencyName = "Bitcoin (BTC)",
                address = btcAddress,
                badgeColor = Color(0xFFF7931A),
                onClick = {
                    handleCryptoClick(context, clipboardManager, "Bitcoin", btcAddress, "bitcoin")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ethereum Card
            CryptoAddressCard(
                currencyName = "Ethereum (ETH)",
                address = ethAddress,
                badgeColor = Color(0xFF627EEA),
                onClick = {
                    handleCryptoClick(context, clipboardManager, "Ethereum", ethAddress, "ethereum")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Solana Card
            CryptoAddressCard(
                currencyName = "Solana (SOL)",
                address = solAddress,
                badgeColor = Color(0xFF14F195),
                onClick = {
                    handleCryptoClick(context, clipboardManager, "Solana", solAddress, "solana")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Litecoin Card
            CryptoAddressCard(
                currencyName = "Litecoin (LTC)",
                address = ltcAddress,
                badgeColor = Color(0xFF345D9D),
                onClick = {
                    handleCryptoClick(context, clipboardManager, "Litecoin", ltcAddress, "litecoin")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Zcash Card
            CryptoAddressCard(
                currencyName = "Zcash (ZEC)",
                address = zecAddress,
                badgeColor = Color(0xFFF4B728),
                onClick = {
                    handleCryptoClick(context, clipboardManager, "Zcash", zecAddress, "zcash")
                }
            )

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
