package it.home.example;

import java.util.ArrayList;

import lombok.Data;

@Data
public class Scene_layout {
	private float id;
	private String catalogEntryId;
	private float storeFrontId;
	private float templateId;
	private ArrayList<Object> subScenes = new ArrayList<Object>();
	private float storeTypeId;
}