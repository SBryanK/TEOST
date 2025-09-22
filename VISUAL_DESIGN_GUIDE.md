# Visual Design Guide (Light-only)

## Typography

- Family: Open Sans via Google Fonts provider (fallback: Noto Sans SC for zh-Hans; final fallback: SansSerif)
- Weights: Headline Bold; Titles/Labels Medium; Body Regular
- Scale (Material 3 defaults adjusted in `Type.kt`)

## Colors

- Brand: TencentBlue as primary accent; white/very light surfaces
- Status: Success/Warning/Error/Info tokens defined in `Color.kt`
- Use gradients sparingly (hero/banner); avoid in controls

## Shapes & Elevation

- Shapes: rounded corners (12–24dp per container hierarchy)
- Elevation: keep mostly flat; cards/CTAs 1–3dp; soft shadows only for emphasis

## Spacing & Touch Targets

- Spacing scale: 4/8/12/16/24/32dp
- Min touch target: 48dp

## Motion

- Enter: 150–250ms ease-out
- Exit: 100–150ms ease-in
- Avoid overshoot for utility actions

## Icons & Illustrations

- Material Symbols + brand icon `teoc_logo`
- Simple vector illustrations (paper plane/folder/cloud) allowed in hero/empty states

## Accessibility

- Target color contrast 4.5:1 for text on surfaces
- Support large font scaling; avoid truncation

## Dark Mode

- Not supported (light-only). See `EdgeOneTheme` forcing light scheme.

## Implementation Pointers

- Colors: `core/ui/theme/Color.kt`
- Typography: `core/ui/theme/Type.kt`
- Shapes: `core/ui/theme/Shapes.kt`
- Theme: `core/ui/theme/Theme.kt`
