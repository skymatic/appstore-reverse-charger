package de.skymatic.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class SettingsProvider {

	private static final String ENV_SETTINGS_PATH = "settingsPath";
	private static final String DEFAULT_SETTINGS_PATH = System.getProperty("java.io.tmpdir");
	private static final Path RELATIVE_HOME_DIR = Path.of("~");
	private static final Path ABSOLUTE_HOME_DIR = Path.of(System.getProperty("user.home"));

	private final Gson gson;
	private final Path path;

	public SettingsProvider() {
		String tempPath = Optional.ofNullable(System.getProperty(ENV_SETTINGS_PATH)).orElse(DEFAULT_SETTINGS_PATH);
		path = replaceHomeDir(Path.of(tempPath));
		gson = new GsonBuilder().setPrettyPrinting().setLenient().registerTypeAdapter(Settings.class, new SettingsJsonAdapter()).create();
	}

	public Settings loadSettings() {
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ); //
			 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			Settings settings = gson.fromJson(reader, Settings.class);
			if (settings == null) {
				throw new IOException("Unexpected EOF");
			}
			return settings;
		} catch (IOException e) {
			return new Settings();
		}
	}

	public void save(Settings settings) throws IOException {
		Files.createDirectories(path.getParent());
		Path tmpPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
		try (OutputStream out = Files.newOutputStream(tmpPath, StandardOpenOption.CREATE_NEW); //
			 Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
			gson.toJson(settings, writer);
		}
		Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
	}

	private Path replaceHomeDir(Path path) {
		if (path.startsWith(RELATIVE_HOME_DIR)) {
			return ABSOLUTE_HOME_DIR.resolve(RELATIVE_HOME_DIR.relativize(path));
		} else {
			return path;
		}
	}
}
