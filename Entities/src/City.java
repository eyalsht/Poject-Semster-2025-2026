package entities;

import java.util.ArrayList;
import java.util.Map;

public class City extends ContentItem
{
    private double priceSub;
    private double priceOneTime;
    private ArrayList<Map> maps;

    public City()
    {
        maps = new ArrayList<Map>();
    }
    public void addMap(Map map)
    {
        maps.add(map);
    }
    public int getMapCount()
    {
        return maps.size();
    }
    @Override
    public String getDetails() {
        return "";
    }
}
