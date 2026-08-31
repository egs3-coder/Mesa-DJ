package com.example.djmesa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicLibraryController {

	private final Path musicDir;

	public MusicLibraryController(@Value("${music.dir:}") String musicDir) {
		this.musicDir = musicDir == null || musicDir.isBlank() ? null : Path.of(musicDir).toAbsolutePath().normalize();
	}

	@GetMapping("/api/library")
	public List<LibrarySong> library(@RequestParam(defaultValue = "") String q) throws IOException {
		if (musicDir == null || !Files.isDirectory(musicDir)) {
			return List.of();
		}

		String query = normalize(q);
		try (var stream = Files.walk(musicDir)) {
			return stream.filter(Files::isRegularFile)
					.filter(this::isAudio)
					.map(this::toSong)
					.filter(song -> query.isBlank() || normalize(song.path()).contains(query)
							|| normalize(song.title()).contains(query) || normalize(song.folder()).contains(query))
					.sorted(Comparator.comparing(LibrarySong::folder).thenComparing(LibrarySong::title))
					.limit(100)
					.toList();
		}
	}

	@GetMapping("/api/library/file")
	public ResponseEntity<Resource> file(@RequestParam String path) {
		if (musicDir == null || !Files.isDirectory(musicDir)) {
			return ResponseEntity.notFound().build();
		}

		Path resolved = musicDir.resolve(path).toAbsolutePath().normalize();
		if (!resolved.startsWith(musicDir) || !Files.isRegularFile(resolved) || !isAudio(resolved)) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok()
				.contentType(mediaType(resolved))
				.body(new FileSystemResource(resolved));
	}

	private LibrarySong toSong(Path path) {
		Path relative = musicDir.relativize(path);
		Path parent = relative.getParent();
		return new LibrarySong(relative.toString().replace('\\', '/'), stripExtension(path.getFileName().toString()),
				parent == null ? "/" : parent.toString().replace('\\', '/'));
	}

	private boolean isAudio(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")
				|| name.endsWith(".flac") || name.endsWith(".aac");
	}

	private String stripExtension(String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	private MediaType mediaType(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (name.endsWith(".mp3")) {
			return MediaType.valueOf("audio/mpeg");
		}
		if (name.endsWith(".wav")) {
			return MediaType.valueOf("audio/wav");
		}
		if (name.endsWith(".ogg")) {
			return MediaType.valueOf("audio/ogg");
		}
		if (name.endsWith(".m4a")) {
			return MediaType.valueOf("audio/mp4");
		}
		if (name.endsWith(".flac")) {
			return MediaType.valueOf("audio/flac");
		}
		if (name.endsWith(".aac")) {
			return MediaType.valueOf("audio/aac");
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}
}
