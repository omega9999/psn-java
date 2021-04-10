package it.home.example;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Attributes {
	private Facets facets;
	private List<Object> next = new ArrayList<>();
}
