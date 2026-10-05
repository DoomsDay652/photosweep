package com.dominic.photosweep;

/** Theme-specific interaction profiles; independent of Android so coverage is testable. */
final class SwipeTheme {
    enum Kind { SPARKLE, FIRE, WATER, ICE, EARTH, LIGHTNING, CANDY, TOXIC, SPACE,
        WEB, GOLD, THUNDER, GAMMA, SHIELD, COSMIC, SCARLET, STEALTH, NEBULA, SOLAR,
        SAKURA, SKY, WISPS, JAPAN, MEXICO, USA, SPAIN, BRAZIL, FRANCE, ITALY, KOREA,
        MY_HERO, BLEACH, ONE_PIECE, NARUTO, DRAGONBALL, YIN_YANG, CHRISTMAS, HALLOWEEN, THANKSGIVING, INDEPENDENCE, NEW_YEAR, MLK, PRESIDENTS, MEMORIAL, JUNETEENTH, LABOR, COLUMBUS, VETERANS }
    static Kind forTheme(int theme) {
        int holiday = HolidayThemes.collection(theme);
        if (holiday >= 0) return Kind.values()[Kind.CHRISTMAS.ordinal() + holiday];
        switch(theme) {
            case 18:return Kind.CANDY; case 19:return Kind.TOXIC; case 20:return Kind.SPACE;
            case 21:return Kind.FIRE; case 22:return Kind.WATER; case 23:return Kind.ICE;
            case 24:return Kind.EARTH; case 25:return Kind.LIGHTNING;
            case 26:case 30:return Kind.SAKURA; case 27:return Kind.SKY; case 31:return Kind.WISPS;
            case 34:return Kind.WEB; case 35:return Kind.GOLD; case 36:return Kind.THUNDER;
            case 37:return Kind.GAMMA; case 38:return Kind.SHIELD; case 39:return Kind.COSMIC;
            case 40:return Kind.SCARLET; case 41:return Kind.STEALTH; case 42:return Kind.NEBULA; case 43:return Kind.SOLAR;
            case 28:case 32:return Kind.JAPAN; case 29:case 33:return Kind.MEXICO;
            case 44:case 50:return Kind.USA; case 45:case 51:return Kind.SPAIN;
            case 46:case 52:return Kind.BRAZIL; case 47:case 53:return Kind.FRANCE;
            case 48:case 54:return Kind.ITALY; case 49:case 55:return Kind.KOREA;
            case 56:return Kind.MY_HERO; case 57:return Kind.BLEACH; case 58:return Kind.ONE_PIECE;
            case 59:return Kind.NARUTO; case 60:return Kind.DRAGONBALL; case 61:return Kind.YIN_YANG;
            default:return Kind.SPARKLE;
        }
    }
    static int color(Kind kind) {
        if (kind.ordinal() >= Kind.CHRISTMAS.ordinal()) {
            int holiday = kind.ordinal() - Kind.CHRISTMAS.ordinal();
            int[] accents = {0xFFFFE0A1, 0xFFD1B8FF, 0xFFFFE0A0, 0xFFF8E1AA, 0xFFFFF1C9, 0xFFF2DEA6, 0xFFEAD49A, 0xFFE8D7B5, 0xFFFFE0A9, 0xFFFFE0A4, 0xFFE6C99A, 0xFFF2DEB0};
            return accents[holiday];
        }
        switch(kind) {
            case FIRE:case SOLAR:case DRAGONBALL:return 0xFFFFA642;
            case WATER:case ONE_PIECE:case SHIELD:return 0xFF83DFFF;
            case ICE:case BLEACH:return 0xFFD5F5FF;
            case EARTH:case BRAZIL:case ITALY:case NARUTO:return 0xFFB5DE83;
            case TOXIC:case GAMMA:case STEALTH:case MY_HERO:return 0xFF9DEE75;
            case CANDY:case SAKURA:case SCARLET:return 0xFFFF9FCA;
            case LIGHTNING:case THUNDER:case NEBULA:case COSMIC:case SKY:case WISPS:return 0xFFB6A8FF;
            case YIN_YANG:return 0xFFE9E9DE;
            default:return 0xFFFFDE97;
        }
    }
    static String description(Kind kind) {
        switch (kind) {
            case CHRISTMAS: return "Hold gathering snow · release a winter flurry";
            case HALLOWEEN: return "Hold bobbing pumpkin lights · release a pumpkin swirl";
            case THANKSGIVING: return "Hold swirling harvest leaves · release an autumn gust";
            case INDEPENDENCE: return "Hold gathering stars · release red, white and blue fireworks";
            case NEW_YEAR: return "Hold golden confetti · release a midnight sparkle";
            case MLK: return "Hold floating doves · release a peaceful breeze";
            case PRESIDENTS: return "Hold sapphire and gold glints · release a laurel flourish";
            case MEMORIAL: return "Hold remembrance poppies · release a gentle petal drift";
            case JUNETEENTH: return "Hold rising stars · release a celebration bloom";
            case LABOR: return "Hold summer blossoms · release a warm breeze";
            case COLUMBUS: return "Hold softly sailing emblems · release an ocean sweep";
            case VETERANS: return "Hold laurel lights · release a gentle gratitude trail";
            default: break;
        }
        switch(kind) {
            case FIRE:return "Hold to burn · release a flame burst";
            case WATER:return "Hold flowing waves · release a splash";
            case ICE:return "Hold growing frost · release crystal shards";
            case EARTH:return "Hold tumbling leaves · release an earthy gust";
            case LIGHTNING:return "Hold crackling bolts · release a lightning strike";
            case CANDY:return "Hold bouncing sweets · release a candy shower";
            case TOXIC:return "Hold dripping ooze · release a slime splash";
            case SPACE:return "Hold orbiting stars · release shooting comets";
            case WEB:return "Hold weaving silk · release a web snap";
            case GOLD:return "Hold radiant glints · release an armor flare";
            case THUNDER:return "Hold storm arcs · release rolling thunder";
            case GAMMA:return "Hold charged energy · release a gamma shockwave";
            case SHIELD:return "Hold rotating shields · release a shield wave";
            case COSMIC:return "Hold orbiting wisps · release a cosmic spiral";
            case SCARLET:return "Hold magic rings · release a scarlet spell";
            case STEALTH:return "Hold linked nodes · release a precision pulse";
            case NEBULA:return "Hold swirling stardust · release a nebula bloom";
            case SOLAR:return "Hold sun rays · release a solar flare";
            case SAKURA:return "Hold drifting petals · release a blossom gust";
            case SKY:return "Hold floating runes · release a sky ripple";
            case WISPS:return "Hold wandering lights · release a spirit bloom";
            case JAPAN:return "Hold swaying lanterns · release a lantern breeze";
            case MEXICO:return "Hold swirling marigolds · release a flower celebration";
            case USA:return "Hold orbiting stars · release a stars-and-flags burst";
            case SPAIN:return "Hold dancing fans · release a fan flourish";
            case BRAZIL:return "Hold swaying palms · release a tropical gust";
            case FRANCE:return "Hold lavender sprigs · release a lavender breeze";
            case ITALY:return "Hold olive branches · release an olive swirl";
            case KOREA:return "Hold floating lanterns · release a lantern spiral";
            case MY_HERO:return "Hold charged green sparks · release a hero impact";
            case BLEACH:return "Hold spirit energy · release a spirit slash";
            case ONE_PIECE:return "Hold rolling seas · release a pirate wave";
            case NARUTO:return "Hold spinning chakra · release a ninja vortex";
            case DRAGONBALL:return "Hold charging ki · release an energy blast";
            case YIN_YANG:return "Hold balanced wisps · release an ink swirl";
            default:return "Hold shimmering sparks · release a sparkle burst";
        }
    }
}
