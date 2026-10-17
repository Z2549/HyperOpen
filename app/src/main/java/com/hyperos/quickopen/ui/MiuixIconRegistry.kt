package com.hyperos.quickopen.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Cut
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.icon.extended.WorldClock

object MiuixIconRegistry {
    val ICONS: List<Pair<String, ImageVector>> = listOf(
        "Scan" to MiuixIcons.Scan,
        "BankCards" to MiuixIcons.BankCards,
        "Photos" to MiuixIcons.Photos,
        "Search" to MiuixIcons.Search,
        "Settings" to MiuixIcons.Settings,
        "Music" to MiuixIcons.Music,
        "Notes" to MiuixIcons.Notes,
        "Contacts" to MiuixIcons.Contacts,
        "Folder" to MiuixIcons.Folder,
        "Home" to MiuixIcons.Home,
        "Email" to MiuixIcons.Email,
        "Phone" to MiuixIcons.Phone,
        "Messages" to MiuixIcons.Messages,
        "WorldClock" to MiuixIcons.WorldClock,
        "Alarm" to MiuixIcons.Alarm,
        "Timer" to MiuixIcons.Timer,
        "Location" to MiuixIcons.Location,
        "Link" to MiuixIcons.Link,
        "Share" to MiuixIcons.Share,
        "Lock" to MiuixIcons.Lock,
        "Unlock" to MiuixIcons.Unlock,
        "Play" to MiuixIcons.Play,
        "Pause" to MiuixIcons.Pause,
        "VolumeUp" to MiuixIcons.VolumeUp,
        "VolumeOff" to MiuixIcons.VolumeOff,
        "Download" to MiuixIcons.Download,
        "Copy" to MiuixIcons.Copy,
        "Cut" to MiuixIcons.Cut,
        "Paste" to MiuixIcons.Paste,
        "Filter" to MiuixIcons.Filter,
        "Sort" to MiuixIcons.Sort,
        "Tune" to MiuixIcons.Tune,
        "Help" to MiuixIcons.Help,
        "Info" to MiuixIcons.Info,
        "Edit" to MiuixIcons.Edit,
        "Add" to MiuixIcons.Add,
        "Delete" to MiuixIcons.Delete,
        "Refresh" to MiuixIcons.Refresh,
    )

    fun find(key: String?): ImageVector? {
        if (key.isNullOrBlank()) return null
        return ICONS.firstOrNull { it.first.equals(key.trim(), ignoreCase = true) }?.second
    }
}
