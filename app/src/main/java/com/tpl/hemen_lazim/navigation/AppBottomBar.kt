package com.tpl.hemen_lazim.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tpl.hemen_lazim.R

enum class TopLevelTab(
    val route: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    EXPLORE(Routes.EXPLORE, R.string.tab_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    MY_REQUESTS(
        Routes.MY_REQUESTS,
        R.string.tab_my_requests,
        Icons.AutoMirrored.Filled.ListAlt,
        Icons.AutoMirrored.Outlined.ListAlt
    ),
    CHATS(Routes.CHATS, R.string.tab_chats, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    PROFILE(Routes.PROFILE, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person);

    companion object {
        val routes = entries.map { it.route }.toSet()
    }
}

/** [unreadTotal]: tüm sohbetlerdeki okunmamış mesaj sayısı, Sohbetler sekmesinde rozet olur. */
@Composable
fun AppBottomBar(currentRoute: String?, unreadTotal: Int, onTabSelected: (TopLevelTab) -> Unit) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onTabSelected(tab) },
                icon = {
                    val icon = if (selected) tab.selectedIcon else tab.icon
                    if (tab == TopLevelTab.CHATS && unreadTotal > 0) {
                        val unreadDescription =
                            pluralStringResource(R.plurals.unread_messages, unreadTotal, unreadTotal)
                        BadgedBox(
                            badge = {
                                Badge(Modifier.semantics { contentDescription = unreadDescription }) {
                                    Text(if (unreadTotal > 99) "99+" else unreadTotal.toString())
                                }
                            }
                        ) { Icon(icon, contentDescription = null) }
                    } else {
                        Icon(icon, contentDescription = null)
                    }
                },
                label = { Text(label) }
            )
        }
    }
}
