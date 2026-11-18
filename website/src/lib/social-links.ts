export interface SocialItem {
  name: string;
  handle?: string;
  href: string;
  paths: readonly string[];
  enabled: boolean;
  category?: "social" | "development" | "community";
}

export const SOCIAL_LINKS: Record<string, SocialItem> = {
  github: {
    name: "GitHub",
    handle: "saad2134/dosezy",
    href: "https://github.com/saad2134/dosezy",
    paths: ["/github", "/gh", "/repo"],
    enabled: true,
    category: "development",
  },
  issues: {
    name: "Issues",
    href: "https://github.com/saad2134/dosezy/issues",
    paths: ["/issues"],
    enabled: true,
    category: "development",
  },
  discussions: {
    name: "Discussions",
    href: "https://github.com/saad2134/dosezy/discussions",
    paths: ["/discussions", "/community"],
    enabled: true,
    category: "community",
  },
  releases: {
    name: "Releases",
    href: "https://github.com/saad2134/dosezy/releases",
    paths: ["/releases"],
    enabled: true,
    category: "development",
  },
  sponsor: {
    name: "Sponsor & Funding",
    href: "https://github.com/sponsors/saad2134",
    paths: ["/sponsor", "/donate"],
    enabled: true,
    category: "development",
  },
  x: {
    name: "X (Twitter)",
    handle: "@dosezyapp",
    href: "https://x.com/dosezyapp",
    paths: ["/twitter", "/x"],
    enabled: true,
    category: "social",
  },
  instagram: {
    name: "Instagram",
    handle: "@dosezyapp",
    href: "https://www.instagram.com/dosezyapp",
    paths: ["/instagram", "/ig"],
    enabled: true,
    category: "social",
  },
  bluesky: {
    name: "Bluesky",
    handle: "@dosezy.attenomy.com",
    href: "https://bsky.app/profile/dosezy.attenomy.com",
    paths: ["/bluesky", "/bsky"],
    enabled: true,
    category: "social",
  },
  /*
  reddit: {
    name: "Reddit",
    handle: "r/Dosezy",
    href: "https://www.reddit.com/r/Dosezy/",
    paths: ["/reddit"],
    enabled: false,
    category: "community",
  },
  */
  youtube: {
    name: "YouTube",
    handle: "@dosezyapp",
    href: "https://www.youtube.com/@dosezyapp",
    paths: ["/youtube", "/yt"],
    enabled: true,
    category: "social",
  },
  facebook: {
    name: "Facebook",
    handle: "@dosezyapp",
    href: "https://www.facebook.com/dosezyapp",
    paths: ["/facebook", "/fb"],
    enabled: true,
    category: "social",
  },
  tiktok: {
    name: "TikTok",
    handle: "@dosezyapp",
    href: "https://www.tiktok.com/@dosezyapp",
    paths: ["/tiktok"],
    enabled: true,
    category: "social",
  },
  // Placeholders for future accounts (currently disabled)
  threads: {
    name: "Threads",
    handle: "@dosezyapp",
    href: "https://www.threads.net/@dosezyapp",
    paths: ["/threads"],
    enabled: false,
    category: "social",
  },
  linkedin: {
    name: "LinkedIn",
    handle: "dosezy",
    href: "https://www.linkedin.com/company/dosezy",
    paths: ["/linkedin"],
    enabled: false,
    category: "social",
  },
};

/**
 * All enabled social links for UI presentation
 */
export const activeSocialLinks = Object.values(SOCIAL_LINKS).filter(
  (item) => item.enabled
);

/**
 * Key-value mapping of redirect path to target external destination for astro.config.mjs
 */
export const socialRedirects: Record<string, string> = Object.values(
  SOCIAL_LINKS
)
  .filter((item) => item.enabled)
  .reduce<Record<string, string>>((acc, item) => {
    for (const path of item.paths) {
      acc[path] = item.href;
    }
    return acc;
  }, {});
