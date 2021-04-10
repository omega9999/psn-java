package it.home.example;

import java.util.ArrayList;

import lombok.Data;

@Data
public class Template_def {
	private String name;
	private float id;
	private float storeTypeId;
	private String imageUrl = null;
	private ArrayList<Object> locations = new ArrayList<Object>();
	private ArrayList<Object> extras = new ArrayList<Object>();

}