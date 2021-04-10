package it.home.example;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * https://store.playstation.com/chihiro-api/viewfinder/IT/it/999/STORE-MSF75508-GAMESPOPULAR
 */
@Data
public class RootCodeBeautify {
	private String id;
	private int age_limit;
	private Attributes attributes;
	private String container_type;
	private int content_origin;
	private boolean dob_required;
	private List<Image> images = new ArrayList<>();
	private List<Link> links = new ArrayList<>();
	private String long_desc;
	private Metadata metadata;
	private String name;
	private List<PromoMedia> promomedia = new ArrayList<>();
	private boolean restricted;
	private int revision;
	private Scene_layout scene_layout;
	private int size;
	private List<SkuLink> sku_links = new ArrayList<>();
	private String sort;
	private int start;
	private Template_def template_def;
	private long timestamp;
	private int total_results;
	private String url;
	private boolean expanded;

}
