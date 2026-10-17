package com.dominic.photosweep;

/** Theme-specific interaction profiles; independent of Android so coverage is testable. */
final class SwipeTheme {
    enum Kind { SPARKLE, FIRE, WATER, ICE, EARTH, LIGHTNING, CANDY, TOXIC, SPACE,
        WEB, GOLD, THUNDER, GAMMA, SHIELD, COSMIC, SCARLET, STEALTH, NEBULA, SOLAR,
        SAKURA, SKY, WISPS, JAPAN, MEXICO, USA, SPAIN, BRAZIL, FRANCE, ITALY, KOREA,
        MY_HERO, BLEACH, ONE_PIECE, NARUTO, DRAGONBALL, YIN_YANG, CHRISTMAS, HALLOWEEN, THANKSGIVING, INDEPENDENCE, NEW_YEAR, MLK, PRESIDENTS, MEMORIAL, JUNETEENTH, LABOR, COLUMBUS, VETERANS, SLASHER, SUMMER }
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
            case 86:return Kind.SLASHER;
            case 87:return Kind.SUMMER;
            case 59:return Kind.NARUTO; case 60:return Kind.DRAGONBALL; case 61:return Kind.YIN_YANG;
            default:return Kind.SPARKLE;
        }
    }
    static int color(Kind kind) {
        if(kind==Kind.SUMMER)return 0xFF83E5E3;
        if(kind==Kind.SLASHER)return 0xFFD87377;
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
        switch(kind) {
            case SLASHER:return "Keep: release the photo to be stabbed at its captured angle and carried away · Trash: walks through an opening door.";
            case HALLOWEEN:return "Trash: a jack-o-lantern devours the photograph · Keep: a ghost carries the photograph into the moonlight.";
            case SUMMER:return "Trash: a shark bites away pieces of the photograph · Keep: rolling surf carries the photograph away.";
            case FIRE:return "Trash: contact burns with angled flames from the side · Keep: photo ignites from bottom to top.";
            case WATER:return "Trash: photo liquefies into streams and droplets · Keep: photo becomes a bubble and floats away.";
            case ICE:return "Trash: touched photo freezes, cracks, then sheds rigid shards · Keep: crystallizes and slides away.";
            case TOXIC:return "Trash: the contacted edge droops into photo-textured strands that drain into the sludge pool · Keep: photo rolls into a vial and tilts into depth.";
            case SPACE:return "Trash: photo slices swirl into the black hole · Keep: a brilliant starburst.";
            case CANDY:return "Trash: compresses, crunches and bounces photo crumbs · Keep: randomly folds the photo into candy shapes.";
            default:break;
        }
        return "Keep: warm glow and sparkles · Trash: photo chips at the left edge. Style changes the sparkle accents.";
    }
}

