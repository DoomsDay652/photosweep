package com.dominic.photosweep;

public final class ThemeCatalogCheck {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(ThemeCatalog.cost(1)==0,"Basic themes are free");check(ThemeCatalog.cost(2)==1,"Scenes cost one Gold");check(ThemeCatalog.cost(3)==3,"Animated themes cost three Diamonds");
        check(ThemeCatalog.price(1).equals("Free")&&ThemeCatalog.price(3).equals("3 Diamonds"),"Visible prices agree with deductions");
        int[] counts=new int[7];for(int i=0;i<ThemeCatalog.COUNT;i++){int group=ThemeCatalog.collection(i);check(group>0&&group<7,"Every theme has one subject");counts[group]++;}
        check(counts[1]==8&&counts[4]==16&&counts[5]==25,"Free, country and holiday ownership IDs retained");
        int[] still={28,29,44,45,46,47,48,49},animated={32,33,50,51,52,53,54,55};
        for(int i=0;i<still.length;i++)check(ThemeCatalog.collection(still[i])==4&&ThemeCatalog.collection(animated[i])==4,"Country variants stay together");
        for(int i=62;i<86;i+=2)check(ThemeCatalog.collection(i)==ThemeCatalog.collection(i+1),"Holiday variants stay together");
        check(ThemeCatalog.collection(86)==5 && PlayPolicy.themeAllowed(86) && SwipeTheme.forTheme(86)==SwipeTheme.Kind.SLASHER,"Slasher is selectable under Holidays");
        check(SeasonalThemes.group(64)==SeasonalThemes.group(65) && SeasonalThemes.group(65)==SeasonalThemes.group(86),"Halloween scenes and Slasher share one family");
        check(java.util.Arrays.equals(SeasonalThemes.variants(1),new int[]{64,65,86}),"Halloween keeps every owned ID");
        check(!PlayPolicy.themeAllowed(87),"Unapproved seasonal draft is excluded");
        check(ThemeCatalog.visible(true,0)&&ThemeCatalog.visible(false,0),"All shows both");check(ThemeCatalog.visible(true,1)&&!ThemeCatalog.visible(false,1),"Owned only");check(ThemeCatalog.visible(false,2)&&!ThemeCatalog.visible(true,2),"Locked only");
        System.out.println("Theme collections, prices and ownership filters passed");
    }
}
