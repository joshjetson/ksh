package ksh

import grails.plugin.springsecurity.annotation.Secured

/**
 * Serves the fixed set of preset profile avatars used when AppConfig.profileUploadEnabled
 * is off. Each preset is a tiny inlined SVG — no asset pipeline, no upload, no DB.
 *
 * Avatar URLs stored on User look like: /avatar/preset/<slug>
 */
@Secured(['ROLE_USER', 'ROLE_ADMIN'])
class AvatarController {

    // Slug -> [character, hex bg, hex fg]. Korean characters chosen for theme.
    private static final Map<String, List<String>> PRESETS = [
        'han'     : ['한', '#fda4af', '#7f1d1d'],
        'geul'    : ['글', '#fcd34d', '#78350f'],
        'hak'     : ['학', '#6ee7b7', '#064e3b'],
        'eo'      : ['어', '#7dd3fc', '#0c4a6e'],
        'chaek'   : ['책', '#c4b5fd', '#3b0764'],
        'byeol'   : ['별', '#f9a8d4', '#831843'],
        'kkot'    : ['꽃', '#fdba74', '#7c2d12'],
        'san'     : ['산', '#d6d3d1', '#1c1917']
    ]

    /**
     * GET /avatar/preset/<slug>  -> SVG of the preset.
     * The slug arrives as params.id via Grails' default
     * /$controller/$action?/$id? mapping.
     */
    def preset() {
        String slug = params.id
        def def_ = PRESETS[slug]
        if (!def_) {
            render status: 404, text: 'Unknown preset'
            return
        }
        String char_ = def_[0]
        String bg = def_[1]
        String fg = def_[2]

        String svg = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80" width="80" height="80">
  <circle cx="40" cy="40" r="40" fill="${bg}"/>
  <text x="40" y="56" font-size="44" font-family="-apple-system, 'Segoe UI', sans-serif"
        font-weight="700" fill="${fg}" text-anchor="middle">${char_}</text>
</svg>"""

        response.contentType = 'image/svg+xml'
        response.setHeader('Cache-Control', 'public, max-age=31536000, immutable')
        render svg
    }

    /**
     * Expose the preset slugs (read by the picker view).
     */
    static List<String> getPresetSlugs() {
        PRESETS.keySet().toList()
    }
}
