package common.dto;

import common.content.Site;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GetSitesResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Site> sites = new ArrayList<>();

    public GetSitesResponse(List<Site> sites) {
        this.sites = (sites != null) ? sites : new ArrayList<>();
    }

    public List<Site> getSites() {
        return sites;
    }
}
