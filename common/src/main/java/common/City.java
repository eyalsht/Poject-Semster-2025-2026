package common;

import java.util.ArrayList;
import java.util.Map;

public class City extends ContentItem
{
    private double priceSub;
    private double priceOneTime;

    private double pendingPriceSub;
    private double pendingPriceOneTime;

    private ArrayList<Map> maps;

    public City() {
        this.maps = new ArrayList<>();
    }
    public City(int id, String name, double priceOneTime, double priceSub) {
        this.id = id;
        this.name = name;
        this.priceOneTime = priceOneTime;
        this.priceSub = priceSub;
        this.maps = new ArrayList<>();
    }
    public double getPriceSub() { return priceSub; }
    public void setPriceSub(double priceSub) { this.priceSub = priceSub; }

    public double getPriceOneTime() { return priceOneTime; }
    public void setPriceOneTime(double priceOneTime) { this.priceOneTime = priceOneTime; }
    public void addMap(Map map) {
        if(this.maps == null) this.maps = new ArrayList<>();
        this.maps.add(map);
    }
    public int getMapCount()
    {
        return maps.size();
    }
    @Override
    public String getDetails() {
        return "City: " + name;
    }
}
