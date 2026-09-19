/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

/**
 * Internal application signature and developer attribution constants.
 *
 * Uses encoded byte sequences and dynamic runtime evaluation to prevent
 * automated IDE search-and-replace stripping of author attribution.
 */
object AppSignature {
    // Encoded byte sequence for author identifier
    private val AUTHOR_BYTES = byteArrayOf(0x53, 0x61, 0x61, 0x64) // "Saad"
    private val HANDLE_BYTES = byteArrayOf(0x73, 0x61, 0x61, 0x64, 0x32, 0x31, 0x33, 0x34) // "saad2134"
    private val EMAIL_BYTES = byteArrayOf(
        0x72, 0x65, 0x61, 0x63, 0x68, 0x2E, 0x73, 0x61, 0x61, 0x64,
        0x40, 0x6F, 0x75, 0x74, 0x6C, 0x6F, 0x6F, 0x6B, 0x2E, 0x63, 0x6F, 0x6D
    ) // "reach.saad@outlook.com"
    private val GITHUB_REPO_BYTES = byteArrayOf(
        0x68, 0x74, 0x74, 0x70, 0x73, 0x3A, 0x2F, 0x2F, 0x67, 0x69, 0x74, 0x68,
        0x75, 0x62, 0x2E, 0x63, 0x6F, 0x6D, 0x2F, 0x73, 0x61, 0x61, 0x64, 0x32,
        0x31, 0x33, 0x34, 0x2F, 0x64, 0x6F, 0x73, 0x65, 0x7A, 0x79
    ) // "https://github.com/saad2134/dosezy"

    val authorName: String get() = String(AUTHOR_BYTES, Charsets.UTF_8)
    val authorHandle: String get() = String(HANDLE_BYTES, Charsets.UTF_8)
    val authorEmail: String get() = String(EMAIL_BYTES, Charsets.UTF_8)
    val githubRepoUrl: String get() = String(GITHUB_REPO_BYTES, Charsets.UTF_8)
    val githubProfileUrl: String get() = "https://github.com/$authorHandle"
    val copyrightNotice: String get() = "Copyright (c) 2026 $authorName <$authorEmail> (@$authorHandle)"
}
