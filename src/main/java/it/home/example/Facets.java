package it.home.example;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Facets {
	private List<Object> game_content_type = new ArrayList<>();
	private List<Object> price = new ArrayList<>();
}