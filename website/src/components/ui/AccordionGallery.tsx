'use client';

import React, { useRef, useEffect, useState, useCallback, type CSSProperties, type KeyboardEvent, type MouseEvent } from 'react';
import { gsap } from 'gsap';

export interface AccordionGalleryItem {
  image: string;
  label?: string;
  link?: string;
  alt?: string;
}

export interface AccordionGalleryProps {
  items?: AccordionGalleryItem[];
  defaultIndex?: number;
  accentColor?: string;
  height?: number;
  gap?: number;
  expandRatio?: number;
  orientation?: 'horizontal' | 'vertical';
  duration?: number;
  ease?: string;
  parallax?: number;
  tilt?: number;
  stagger?: number;
  trigger?: 'hover' | 'click';
  showLabels?: boolean;
  className?: string;
}

const DEFAULT_ITEMS: AccordionGalleryItem[] = [
  { image: '/screenshots/home.png', label: 'Home Screen', alt: 'Dosezy Home Screen' },
  { image: '/screenshots/schedule.png', label: 'Daily Schedule', alt: 'Dosezy Daily Schedule' },
  { image: '/screenshots/medicines.png', label: 'Medicines Cabinet', alt: 'Dosezy Medicines Cabinet' },
  { image: '/screenshots/add-med.png', label: 'Add Medicine', alt: 'Dosezy Add Medicine' },
  { image: '/screenshots/refill.png', label: 'Notification Reliability', alt: 'Dosezy Notification Reliability' },
  { image: '/screenshots/reliability.png', label: 'Customizable Preferences', alt: 'Dosezy Customizable Preferences' }
];

export const AccordionGallery = ({
  items = DEFAULT_ITEMS,
  defaultIndex = 0,
  accentColor = '#38bdf8',
  height = 550,
  gap = 8,
  expandRatio = 0.48,
  orientation = 'horizontal',
  duration = 0.55,
  ease = 'power3.out',
  parallax = 0.35,
  tilt = 6,
  stagger = 0.05,
  trigger = 'hover',
  showLabels = true,
  className = ''
}: AccordionGalleryProps) => {
  const rootRef = useRef<HTMLDivElement>(null);
  const panelRefs = useRef<(HTMLElement | null)[]>([]);
  const mediaRefs = useRef<(HTMLImageElement | null)[]>([]);
  const labelRefs = useRef<(HTMLDivElement | null)[]>([]);
  const tlRef = useRef<gsap.core.Timeline | null>(null);
  const firstRunRef = useRef(true);

  const vertical = orientation === 'vertical';
  const count = items.length;
  const [active, setActive] = useState(Math.min(Math.max(defaultIndex, 0), count - 1));

  const prefersReduced =
    typeof window !== 'undefined' && window.matchMedia
      ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
      : false;

  const applyLayout = useCallback(
    (animate: boolean) => {
      const panels = panelRefs.current;
      if (!panels.length) return;

      const r = Math.min(Math.max(expandRatio, 0.25), 0.85);
      const grow = count > 1 ? (r * (count - 1)) / (1 - r) : 1;

      tlRef.current?.kill();
      const dur = animate && !prefersReduced ? duration : 0;
      const tl = gsap.timeline();

      panels.forEach((panel, i) => {
        if (!panel) return;
        const isActive = i === active;
        const media = mediaRefs.current[i];
        const label = labelRefs.current[i];

        const rot = isActive ? 0 : i < active ? tilt : -tilt;
        const rotProp = vertical ? { rotateX: -rot } : { rotateY: rot };

        // Animate panel flex-grow and 3D perspective rotation
        tl.to(
          panel,
          {
            flexGrow: isActive ? grow : 1,
            borderColor: isActive ? 'rgba(255, 255, 255, 0.3)' : 'rgba(255, 255, 255, 0.1)',
            ...rotProp,
            duration: dur,
            ease
          },
          0
        );

        // Animate inner screenshot parallax drift and subtle zoom
        if (media) {
          const drift = Math.max(-1.5, Math.min(1.5, active - i));
          const shift = drift * parallax * 12;
          tl.to(
            media,
            {
              x: vertical ? 0 : isActive ? 0 : shift,
              y: vertical ? (isActive ? 0 : shift) : 0,
              scale: isActive ? 1 : 1.05,
              duration: dur,
              ease
            },
            0
          );
        }

        // Animate squircle label pill reveal on active screenshot
        if (showLabels && label) {
          if (isActive) {
            tl.to(
              label,
              {
                opacity: 1,
                y: 0,
                duration: dur,
                ease,
                delay: prefersReduced ? 0 : stagger
              },
              0
            );
          } else {
            tl.to(
              label,
              {
                opacity: 0,
                y: 6,
                duration: dur * 0.45,
                ease
              },
              0
            );
          }
        }
      });

      tlRef.current = tl;
    },
    [
      active,
      count,
      expandRatio,
      duration,
      ease,
      vertical,
      tilt,
      parallax,
      showLabels,
      stagger,
      prefersReduced
    ]
  );

  useEffect(() => {
    applyLayout(!firstRunRef.current);
    firstRunRef.current = false;
  }, [applyLayout]);

  useEffect(
    () => () => {
      tlRef.current?.kill();
    },
    []
  );

  const handleEnter = (i: number) => {
    if (trigger === 'hover') setActive(i);
  };

  const handleClick = (i: number, e: MouseEvent) => {
    if (i !== active) {
      e.preventDefault();
      setActive(i);
    }
  };

  const handleKeyDown = (i: number, e: KeyboardEvent) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((i + 1) % count);
    } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((i - 1 + count) % count);
    }
  };

  return (
    <div
      ref={rootRef}
      className={`flex ${vertical ? 'flex-col' : 'flex-row'} w-full max-w-full [perspective:1400px] max-[540px]:!flex-col max-[540px]:[perspective:none] ${className}`}
      style={{
        gap: `${gap}px`,
        height: vertical ? `${Math.round(height * 1.5)}px` : `${height}px`
      }}
      role="list"
      aria-label="Screenshots showcase gallery"
    >
      {items.map((item, i) => {
        const isActive = i === active;
        const Tag = (item.link ? 'a' : 'div') as 'a';
        return (
          <Tag
            key={i}
            ref={(el: HTMLElement | null) => {
              panelRefs.current[i] = el;
            }}
            className={`group relative block min-w-0 min-h-0 flex-[1_1_0] cursor-pointer overflow-hidden rounded-[20px] sm:rounded-[22px] bg-black no-underline outline-none [transform-style:preserve-3d] [transform-origin:center] transition-colors duration-300 border ${
              isActive
                ? 'border-white/30 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.9),inset_0_1px_1px_rgba(255,255,255,0.2)]'
                : 'border-white/10 hover:border-white/20'
            } max-[540px]:min-h-[80px] max-[540px]:!transform-none`}
            style={
              {
                willChange: 'flex-grow, transform'
              } as CSSProperties
            }
            href={item.link || undefined}
            onClick={e => handleClick(i, e)}
            onMouseEnter={() => handleEnter(i)}
            onFocus={() => setActive(i)}
            onKeyDown={e => handleKeyDown(i, e)}
            role="listitem"
            tabIndex={0}
            aria-current={isActive ? 'true' : undefined}
            aria-label={item.label}
          >
            {/* Screenshot Image Surface */}
            <div className="relative w-full h-full overflow-hidden">
              <img
                ref={(el: HTMLImageElement | null) => {
                  mediaRefs.current[i] = el;
                }}
                src={item.image}
                alt={item.alt || item.label || ''}
                draggable={false}
                className={`w-full h-full object-cover select-none pointer-events-none [-webkit-user-drag:none] transition-[filter,opacity] duration-500 ease-out ${
                  isActive
                    ? 'grayscale-0 blur-none opacity-100'
                    : 'grayscale blur-[2px] opacity-50'
                }`}
                style={{
                  willChange: 'transform'
                }}
              />

              {/* Inactive Panel Dimmer Overlay for legibility and depth */}
              <div
                className={`absolute inset-0 bg-[#07090e]/40 pointer-events-none transition-opacity duration-300 ${
                  isActive ? 'opacity-0' : 'opacity-100'
                }`}
              />

              {/* Glass Reflection Glare on Active Frame */}
              <div
                className={`absolute inset-0 bg-gradient-to-tr from-transparent via-white/[0.04] to-transparent pointer-events-none transition-opacity duration-300 ${
                  isActive ? 'opacity-100' : 'opacity-0'
                }`}
              />
            </div>

            {/* Squircle Caption Badge for active screen */}
            {showLabels && item.label && (
              <div
                ref={(el: HTMLDivElement | null) => {
                  labelRefs.current[i] = el;
                }}
                className="pointer-events-none absolute bottom-4 left-1/2 -translate-x-1/2 z-30 flex items-center px-3 py-1 rounded-xl bg-[#030407]/92 backdrop-blur-md border border-white/15 shadow-xl whitespace-nowrap opacity-0"
                aria-hidden="true"
              >
                <span className="text-[11px] sm:text-xs font-semibold tracking-wide text-neutral-200">
                  {item.label}
                </span>
              </div>
            )}
          </Tag>
        );
      })}
    </div>
  );
};

export default AccordionGallery;
