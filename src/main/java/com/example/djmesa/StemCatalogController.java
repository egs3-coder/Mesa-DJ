package com.example.djmesa;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StemCatalogController {

	private static final List<StemTrack> TRACKS = List.of(
			new StemTrack("kick", "Bateria", "Kick", false),
			new StemTrack("caixa", "Bateria", "Caixa", false),
			new StemTrack("hats", "Bateria", "Hats", false),
			new StemTrack("surdos", "Bateria", "Surdos", false),
			new StemTrack("guitarra-b", "Instrumental", "Guitarra B", false),
			new StemTrack("guitarra-s", "Instrumental", "Guitarra S", false),
			new StemTrack("baixo", "Instrumental", "Baixo", false),
			new StemTrack("cordas-piano", "Instrumental", "Cordas / Piano", false),
			new StemTrack("vocals", "Vocals", "Vocais completos", false),
			new StemTrack("lead-vocal", "Vocal principal", "Somente vocal principal", true),
			new StemTrack("backing-vocals", "Backing Vocals", "Backing vocals", false));

	@GetMapping("/api/stems")
	public List<StemTrack> stems() {
		return TRACKS;
	}
}
