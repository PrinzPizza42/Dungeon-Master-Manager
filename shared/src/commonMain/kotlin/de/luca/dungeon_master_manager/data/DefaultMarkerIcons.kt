package de.luca.dungeon_master_manager.data

import java.io.File

object DefaultMarkerIcons {

    data class DefaultIcon(
        val fileName: String,
        val svgContent: String
    )

    val ICONS: List<DefaultIcon> = listOf(
        DefaultIcon(
            fileName = "settlement.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Left house -->
                        <rect x="10" y="55" width="25" height="32" fill="#E2E8F0" stroke="#0F172A" stroke-width="3" stroke-linejoin="round"/>
                        <polygon points="8,55 22,35 37,55" fill="#D97706" stroke="#0F172A" stroke-width="3" stroke-linejoin="round"/>
                        <rect x="18" y="68" width="8" height="19" fill="#78350F"/>
                        
                        <!-- Church / Spire -->
                        <rect x="68" y="38" width="22" height="49" fill="#CBD5E1" stroke="#0F172A" stroke-width="3" stroke-linejoin="round"/>
                        <polygon points="66,38 79,12 92,38" fill="#B45309" stroke="#0F172A" stroke-width="3" stroke-linejoin="round"/>
                        <circle cx="79" cy="50" r="4" fill="#0F172A"/>

                        <!-- Center Townhall -->
                        <rect x="28" y="46" width="44" height="41" fill="#F8FAFC" stroke="#0F172A" stroke-width="3.5" stroke-linejoin="round"/>
                        <polygon points="24,46 50,18 76,46" fill="#EA580C" stroke="#0F172A" stroke-width="3.5" stroke-linejoin="round"/>
                        <!-- Windows -->
                        <rect x="36" y="52" width="8" height="9" rx="1" fill="#0284C7" stroke="#0F172A" stroke-width="2"/>
                        <rect x="56" y="52" width="8" height="9" rx="1" fill="#0284C7" stroke="#0F172A" stroke-width="2"/>
                        <!-- Gate -->
                        <path d="M44,87 L44,69 C44,65 56,65 56,69 L56,87 Z" fill="#78350F" stroke="#0F172A" stroke-width="2.5"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "castle.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Main Fortress Wall -->
                        <rect x="25" y="42" width="50" height="46" fill="#94A3B8" stroke="#0F172A" stroke-width="3.5"/>
                        <!-- Wall Crenellations -->
                        <rect x="34" y="36" width="7" height="8" fill="#94A3B8" stroke="#0F172A" stroke-width="2"/>
                        <rect x="46" y="36" width="8" height="8" fill="#94A3B8" stroke="#0F172A" stroke-width="2"/>
                        <rect x="59" y="36" width="7" height="8" fill="#94A3B8" stroke="#0F172A" stroke-width="2"/>
                        
                        <!-- Left Tower -->
                        <rect x="10" y="28" width="22" height="60" fill="#64748B" stroke="#0F172A" stroke-width="3.5"/>
                        <polygon points="8,28 8,20 14,20 14,24 18,24 18,20 24,20 24,24 28,24 28,20 34,20 34,28" fill="#475569" stroke="#0F172A" stroke-width="2"/>
                        <rect x="18" y="40" width="6" height="12" rx="2" fill="#0F172A"/>

                        <!-- Right Tower -->
                        <rect x="68" y="28" width="22" height="60" fill="#64748B" stroke="#0F172A" stroke-width="3.5"/>
                        <polygon points="66,28 66,20 72,20 72,24 76,24 76,20 82,20 82,24 86,24 86,20 92,20 92,28" fill="#475569" stroke="#0F172A" stroke-width="2"/>
                        <rect x="76" y="40" width="6" height="12" rx="2" fill="#0F172A"/>

                        <!-- Central Keep Tower -->
                        <rect x="38" y="16" width="24" height="28" fill="#94A3B8" stroke="#0F172A" stroke-width="3"/>
                        <polygon points="36,16 36,10 42,10 42,13 46,13 46,10 54,10 54,13 58,13 58,10 64,10 64,16" fill="#475569" stroke="#0F172A" stroke-width="2"/>
                        <polygon points="50,2 40,10 60,10" fill="#DC2626" stroke="#0F172A" stroke-width="2"/>

                        <!-- Gate -->
                        <path d="M42,88 L42,66 C42,60 58,60 58,66 L58,88 Z" fill="#1E293B" stroke="#0F172A" stroke-width="3"/>
                        <line x1="42" y1="72" x2="58" y2="72" stroke="#94A3B8" stroke-width="2"/>
                        <line x1="42" y1="80" x2="58" y2="80" stroke="#94A3B8" stroke-width="2"/>
                        <line x1="50" y1="62" x2="50" y2="88" stroke="#94A3B8" stroke-width="2"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "dungeon.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Outer Cave / Rock Formation -->
                        <path d="M12,88 Q10,38 35,16 Q50,6 68,16 Q90,38 88,88 Z" fill="#334155" stroke="#0F172A" stroke-width="4" stroke-linejoin="round"/>
                        <path d="M22,88 Q20,44 40,24 Q52,14 62,24 Q78,44 78,88 Z" fill="#475569"/>

                        <!-- Archway Bricks -->
                        <path d="M26,88 Q26,32 50,32 Q74,32 74,88" fill="none" stroke="#94A3B8" stroke-width="8" stroke-dasharray="14 3"/>

                        <!-- Portal Abyss -->
                        <path d="M30,88 L30,55 C30,40 70,40 70,55 L70,88 Z" fill="#09090B" stroke="#0F172A" stroke-width="3"/>

                        <!-- Stairs into darkness -->
                        <polygon points="32,88 36,80 64,80 68,88" fill="#1E293B"/>
                        <polygon points="37,80 40,73 60,73 63,80" fill="#334155"/>
                        <polygon points="41,73 44,67 56,67 59,73" fill="#475569"/>

                        <!-- Glowing red eyes in deep cavern -->
                        <circle cx="46" cy="56" r="2.5" fill="#EF4444"/>
                        <circle cx="54" cy="56" r="2.5" fill="#EF4444"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "skull.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Crossed Swords Behind -->
                        <path d="M12,12 L88,88 M88,12 L12,88" stroke="#94A3B8" stroke-width="6" stroke-linecap="round"/>
                        <path d="M12,12 L88,88 M88,12 L12,88" stroke="#0F172A" stroke-width="1.5" stroke-linecap="round"/>
                        <!-- Pommels & Guards -->
                        <circle cx="12" cy="12" r="5" fill="#F59E0B" stroke="#0F172A" stroke-width="2"/>
                        <circle cx="88" cy="12" r="5" fill="#F59E0B" stroke="#0F172A" stroke-width="2"/>
                        <line x1="16" y1="26" x2="26" y2="16" stroke="#F59E0B" stroke-width="4" stroke-linecap="round"/>
                        <line x1="84" y1="26" x2="74" y2="16" stroke="#F59E0B" stroke-width="4" stroke-linecap="round"/>

                        <!-- Skull Dome -->
                        <path d="M24,42 C24,18 76,18 76,42 C76,52 70,58 66,62 L66,74 C66,78 60,80 58,80 L42,80 C40,80 34,78 34,74 L34,62 C30,58 24,52 24,42 Z" fill="#F8FAFC" stroke="#0F172A" stroke-width="4" stroke-linejoin="round"/>
                        
                        <!-- Eye Sockets -->
                        <ellipse cx="38" cy="45" rx="7" ry="9" fill="#0F172A" transform="rotate(-6 38 45)"/>
                        <ellipse cx="62" cy="45" rx="7" ry="9" fill="#0F172A" transform="rotate(6 62 45)"/>
                        
                        <!-- Glowing pupils -->
                        <circle cx="39" cy="45" r="2.5" fill="#EF4444"/>
                        <circle cx="61" cy="45" r="2.5" fill="#EF4444"/>

                        <!-- Nose -->
                        <polygon points="50,54 46,62 54,62" fill="#0F172A"/>

                        <!-- Teeth -->
                        <line x1="42" y1="72" x2="42" y2="80" stroke="#0F172A" stroke-width="2.5"/>
                        <line x1="50" y1="70" x2="50" y2="80" stroke="#0F172A" stroke-width="2.5"/>
                        <line x1="58" y1="72" x2="58" y2="80" stroke="#0F172A" stroke-width="2.5"/>
                        <line x1="37" y1="74" x2="63" y2="74" stroke="#0F172A" stroke-width="2"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "chest.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Glow behind open treasure -->
                        <circle cx="50" cy="50" r="34" fill="#FEF08A" opacity="0.4"/>
                        
                        <!-- Chest Body -->
                        <rect x="14" y="46" width="72" height="42" rx="4" fill="#854D0E" stroke="#0F172A" stroke-width="4"/>
                        <!-- Chest Planks -->
                        <line x1="14" y1="64" x2="86" y2="64" stroke="#5C330A" stroke-width="2"/>
                        <line x1="14" y1="76" x2="86" y2="76" stroke="#5C330A" stroke-width="2"/>

                        <!-- Domed Lid -->
                        <path d="M12,46 Q50,14 88,46 Z" fill="#A16207" stroke="#0F172A" stroke-width="4"/>
                        <path d="M16,46 Q50,22 84,46" fill="none" stroke="#713F12" stroke-width="3"/>

                        <!-- Gold/Brass Straps -->
                        <rect x="26" y="27" width="10" height="61" fill="#F59E0B" stroke="#0F172A" stroke-width="3"/>
                        <rect x="64" y="27" width="10" height="61" fill="#F59E0B" stroke="#0F172A" stroke-width="3"/>

                        <!-- Center Lock Plaque -->
                        <rect x="44" y="42" width="12" height="18" rx="2" fill="#F59E0B" stroke="#0F172A" stroke-width="3"/>
                        <circle cx="50" cy="48" r="2.5" fill="#0F172A"/>
                        <polygon points="49,48 51,48 52,54 48,54" fill="#0F172A"/>

                        <!-- Sparkles -->
                        <polygon points="50,8 52,18 62,20 52,22 50,32 48,22 38,20 48,18" fill="#FDE047"/>
                        <polygon points="82,24 83,28 87,29 83,30 82,34 81,30 77,29 81,28" fill="#FDE047"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "quest.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Badge Shield / Diamond -->
                        <polygon points="50,6 88,24 88,64 50,94 12,64 12,24" fill="#F59E0B" stroke="#0F172A" stroke-width="4" stroke-linejoin="round"/>
                        <polygon points="50,14 80,28 80,60 50,84 20,60 20,28" fill="#FDE047" stroke="#CA8A04" stroke-width="2" stroke-linejoin="round"/>

                        <!-- Exclamation Point -->
                        <path d="M44,26 L56,26 L54,58 L46,58 Z" fill="#0F172A"/>
                        <circle cx="50" cy="70" r="5" fill="#0F172A"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "campfire.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Stone Ring -->
                        <ellipse cx="50" cy="84" rx="38" ry="12" fill="#475569" stroke="#0F172A" stroke-width="3"/>
                        <ellipse cx="20" cy="82" rx="8" ry="5" fill="#64748B" stroke="#0F172A" stroke-width="2"/>
                        <ellipse cx="38" cy="88" rx="9" ry="5" fill="#64748B" stroke="#0F172A" stroke-width="2"/>
                        <ellipse cx="62" cy="88" rx="9" ry="5" fill="#64748B" stroke="#0F172A" stroke-width="2"/>
                        <ellipse cx="80" cy="82" rx="8" ry="5" fill="#64748B" stroke="#0F172A" stroke-width="2"/>

                        <!-- Crossed Logs -->
                        <line x1="22" y1="84" x2="78" y2="68" stroke="#78350F" stroke-width="9" stroke-linecap="round"/>
                        <line x1="78" y1="84" x2="22" y2="68" stroke="#78350F" stroke-width="9" stroke-linecap="round"/>
                        <line x1="22" y1="84" x2="78" y2="68" stroke="#0F172A" stroke-width="1.5" stroke-linecap="round"/>
                        <line x1="78" y1="84" x2="22" y2="68" stroke="#0F172A" stroke-width="1.5" stroke-linecap="round"/>

                        <!-- Outer Flame -->
                        <path d="M50,12 C58,26 76,46 76,64 C76,78 64,84 50,84 C36,84 24,78 24,64 C24,46 42,26 50,12 Z" fill="#DC2626" stroke="#0F172A" stroke-width="3"/>

                        <!-- Middle Flame -->
                        <path d="M50,28 C56,38 68,52 68,66 C68,76 60,82 50,82 C40,82 32,76 32,66 C32,52 44,38 50,28 Z" fill="#F97316"/>

                        <!-- Inner Core Flame -->
                        <path d="M50,44 C54,52 60,62 60,70 C60,77 55,80 50,80 C45,80 40,77 40,70 C40,62 46,52 50,44 Z" fill="#FEF08A"/>

                        <!-- Floating Sparks -->
                        <circle cx="34" cy="24" r="2" fill="#FDE047"/>
                        <circle cx="66" cy="20" r="2.5" fill="#F97316"/>
                        <circle cx="56" cy="8" r="1.5" fill="#FDE047"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "tavern.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Tankard Handle -->
                        <path d="M68,36 C86,36 86,72 68,72" fill="none" stroke="#64748B" stroke-width="8" stroke-linecap="round"/>
                        <path d="M68,36 C86,36 86,72 68,72" fill="none" stroke="#0F172A" stroke-width="2" stroke-linecap="round"/>

                        <!-- Mug Body -->
                        <rect x="22" y="30" width="48" height="56" rx="4" fill="#D97706" stroke="#0F172A" stroke-width="4"/>
                        <!-- Beer Texture / Glass Ribs -->
                        <line x1="34" y1="34" x2="34" y2="82" stroke="#B45309" stroke-width="3"/>
                        <line x1="46" y1="34" x2="46" y2="82" stroke="#B45309" stroke-width="3"/>
                        <line x1="58" y1="34" x2="58" y2="82" stroke="#B45309" stroke-width="3"/>

                        <!-- Frothy Foam Top -->
                        <path d="M18,32 C16,24 24,16 32,18 C36,12 46,10 52,14 C58,10 68,12 70,18 C78,16 84,24 80,32 C74,36 24,36 18,32 Z" fill="#FFFFFF" stroke="#0F172A" stroke-width="3.5"/>
                        <!-- Overflowing Foam Drip -->
                        <path d="M30,32 Q32,46 36,46 Q40,46 40,32" fill="#FFFFFF" stroke="#0F172A" stroke-width="2"/>
                        <path d="M52,32 Q54,42 58,42 Q62,42 62,32" fill="#FFFFFF" stroke="#0F172A" stroke-width="2"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "tower.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Floating Arcane Orb at Peak -->
                        <circle cx="50" cy="10" r="7" fill="#A855F7" stroke="#E9D5FF" stroke-width="2"/>
                        <circle cx="50" cy="10" r="12" fill="#C084FC" opacity="0.3"/>

                        <!-- Tower Spire Roof -->
                        <polygon points="50,18 32,46 68,46" fill="#4338CA" stroke="#0F172A" stroke-width="3.5" stroke-linejoin="round"/>

                        <!-- Tower Upper Gallery -->
                        <rect x="28" y="46" width="44" height="8" fill="#312E81" stroke="#0F172A" stroke-width="3"/>

                        <!-- Main Tower Shaft (Tapered) -->
                        <polygon points="32,54 68,54 74,90 26,90" fill="#64748B" stroke="#0F172A" stroke-width="4" stroke-linejoin="round"/>

                        <!-- Glowing Magic Windows -->
                        <polygon points="46,58 54,58 54,66 46,66" fill="#A855F7" stroke="#0F172A" stroke-width="2"/>
                        <polygon points="46,72 54,72 54,80 46,80" fill="#A855F7" stroke="#0F172A" stroke-width="2"/>

                        <!-- Base Foundation -->
                        <rect x="22" y="90" width="56" height="6" rx="2" fill="#334155" stroke="#0F172A" stroke-width="3"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "ruins.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Foundation Base -->
                        <rect x="10" y="82" width="80" height="10" fill="#64748B" stroke="#0F172A" stroke-width="3.5"/>

                        <!-- Left Pillar (Standing) -->
                        <rect x="18" y="28" width="14" height="54" fill="#94A3B8" stroke="#0F172A" stroke-width="3"/>
                        <rect x="14" y="24" width="22" height="6" fill="#CBD5E1" stroke="#0F172A" stroke-width="2"/>
                        <!-- Pillar cracks -->
                        <path d="M22,40 L28,46 L24,52" fill="none" stroke="#475569" stroke-width="1.5"/>

                        <!-- Architrave / Broken Top Slab -->
                        <polygon points="12,16 54,16 48,24 14,24" fill="#CBD5E1" stroke="#0F172A" stroke-width="3"/>

                        <!-- Center Pillar (Broken Halfway) -->
                        <polygon points="44,52 50,48 54,54 58,50 58,82 44,82" fill="#94A3B8" stroke="#0F172A" stroke-width="3"/>

                        <!-- Right Pillar (Collapsed Stump + Fallen Chunk) -->
                        <rect x="70" y="64" width="14" height="18" fill="#94A3B8" stroke="#0F172A" stroke-width="3"/>
                        <polygon points="62,82 74,70 82,78 70,90" fill="#64748B" stroke="#0F172A" stroke-width="2.5" transform="rotate(25 72 80)"/>

                        <!-- Ivy / Vines on Stone -->
                        <path d="M18,60 Q12,68 18,74 Q24,78 20,82" fill="none" stroke="#10B981" stroke-width="3"/>
                        <circle cx="14" cy="66" r="3" fill="#059669"/>
                        <circle cx="21" cy="76" r="3" fill="#059669"/>
                    </g>
                </svg>
            """.trimIndent()
        ),
        DefaultIcon(
            fileName = "pin.svg",
            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.4"/>
                    </filter>
                    <g filter="url(#shadow)">
                        <!-- Pin Teardrop Body -->
                        <path d="M50,92 C50,92 84,56 84,36 C84,17 69,2 50,2 C31,2 16,17 16,36 C16,56 50,92 50,92 Z" fill="#EF4444" stroke="#0F172A" stroke-width="4" stroke-linejoin="round"/>
                        
                        <!-- Inner White Center -->
                        <circle cx="50" cy="36" r="16" fill="#FFFFFF" stroke="#0F172A" stroke-width="3"/>

                        <!-- Center Star/Dot -->
                        <circle cx="50" cy="36" r="7" fill="#EF4444"/>
                    </g>
                </svg>
            """.trimIndent()
        )
    )

    /**
     * Unpacks all default icons into [targetDir] if they do not exist already.
     */
    fun ensureDefaultIcons(targetDir: File) {
        targetDir.mkdirs()
        for (icon in ICONS) {
            val file = File(targetDir, icon.fileName)
            if (!file.exists() || file.readText().contains("width=\"100\" height=\"100\"")) {
                try {
                    file.writeText(icon.svgContent, Charsets.UTF_8)
                } catch (_: Exception) {}
            }
        }
    }
}
