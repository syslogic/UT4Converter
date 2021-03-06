package org.xtx.ut4converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import javafx.scene.control.TableView;

import javax.imageio.spi.IIORegistry;
import javax.xml.bind.JAXBException;

import org.xtx.ut4converter.UTGames.UTGame;
import org.xtx.ut4converter.UTGames.UnrealEngine;
import org.xtx.ut4converter.config.UserConfig;
import org.xtx.ut4converter.config.UserGameConfig;
import org.xtx.ut4converter.export.CopyExporter;
import org.xtx.ut4converter.export.SimpleTextureExtractor;
import org.xtx.ut4converter.export.UCCExporter;
import org.xtx.ut4converter.export.UModelExporter;
import org.xtx.ut4converter.export.UTPackageExtractor;
import org.xtx.ut4converter.t3d.T3DLevelConvertor;
import org.xtx.ut4converter.t3d.T3DMatch;
import org.xtx.ut4converter.t3d.T3DRessource;
import org.xtx.ut4converter.t3d.T3DRessource.Type;
import org.xtx.ut4converter.t3d.T3DUtils;
import org.xtx.ut4converter.tools.Installation;
import org.xtx.ut4converter.tools.UIUtils;
import org.xtx.ut4converter.tools.psk.Material;
import org.xtx.ut4converter.tools.psk.PSKStaticMesh;
import org.xtx.ut4converter.ucore.UPackage;
import org.xtx.ut4converter.ucore.UPackageRessource;
import org.xtx.ut4converter.ui.ConversionViewController;
import org.xtx.ut4converter.ui.TableRowLog;

/**
 * 
 * @author XtremeXp
 */
@SuppressWarnings("restriction")
public class MapConverter extends Task<T3DLevelConvertor> {

	/**
	 * UT Game the map will be converted from
	 */
	UTGame inputGame;

	/**
	 * UT Game the map will be converted to
	 */
	UTGame outputGame;

	/**
	 * Default sub-folder of UT4 Converter where converted maps will be saved
	 */
	public static final String CONV_PATH = File.separator + "Converted";

	/**
	 * Input map. Can be either a map (.unr, ...) or unreal text map (.t3d)
	 */
	private File inMap;
	
	
	/**
	 * File for logging all events during conversion process
	 */
	private File logFile;

	/**
	 * file writer of logfile
	 */
	private FileWriter logFileWriter;
	
	/**
	 * Buffered writer of logfile
	 */
	private BufferedWriter logBuffWriter;

	/**
	 * Final map name, might differ from original one. E.G: AS-Mazon (UT99) ->
	 * AS-Mazon-Original (for UT4 for exemple)
	 */
	String mapName;

	/**
	 * Relative subfolder from: <UT4Folder>/UnrealTournament/Content path
	 * 
	 * E.g: "MyMaps" would give /UnrealTournament/Content/MyMaps path
	 * 
	 * but /Game/MyMaps for .t3d actor path
	 * 
	 */
	String relativeUtMapPath;
	
	/**
	 * getMapConvertFolder().getAbsolutePath() + File.separator + ressource.getType().getName() + File.separator
	 */
	String ut4ReferenceBaseFolder;

	File inT3d, outT3d;

	/**
	 * Where all converted stuff will be converted
	 */
	Path outPath;

	/**
	 * Scale factor applied to converted level.
	 */
	Double scale;

	/**
	 * Quick converter of actor name. E.G: "Mover" (U1/UT99) -> "InterpActor"
	 * (UT3)
	 */
	T3DMatch tm;

	/**
	 * Tells whether or not map is team based
	 */
	private Boolean isTeamGameType;

	/**
	 * TODO move this to T3D Level converter
	 */
	SupportedClasses supportedActorClasses;

	/**
	 * T3d level converter
	 */
	T3DLevelConvertor t3dLvlConvertor;

	/**
	 * Actor classes that should be converted. If null or empty then all classes
	 * will be converted.
	 */
	private String[] filteredClasses;

	/**
	 * If <code>true</code> textures of the map will be exported and converted.
	 */
	private boolean convertTextures = true;

	/**
	 * If <code>true</code> sounds of the map will be exported and converted
	 */
	private boolean convertSounds = true;

	/**
	 * Changes sound volume of sound actors. For exemple, if soundVolumeRatio <
	 * 100%, volume will be decreased
	 */
	public Float soundVolumeFactor;

	/**
	 * Changes sound volume of sound actors. For exemple, if soundVolumeRatio <
	 * 100%, volume will be decreased
	 */
	public Float brightnessFactor;

	/**
	 * If <code>true</code> staticmeshes of the map will be exported and
	 * converted
	 */
	private boolean convertStaticMeshes = true;

	/**
	 * If <code>true</code> music of the map will be exported and converted
	 */
	private boolean convertMusic = true;

	/**
	 * Allow to extract packages. There should be always only one instanced
	 */
	public List<UTPackageExtractor> packageExtractors;

	public Collection<File> packageFilesCache = new ArrayList<>();

	/**
	 * Unreal packages used in map Can be sounds, textures, ...
	 */
	public Map<String, UPackage> mapPackages = new HashMap<>();

	/**
	 * User configuration which allows to know where UT games are installed for
	 * exemple
	 */
	UserConfig userConfig;

	/**
	 * If true will create notes for unconverted actors in level
	 */
	private boolean createNoteForUnconvertedActors = true;

	/**
	 * Reference to user interface
	 */
	ConversionViewController conversionViewController;

	/**
	 * .t3d map file generated by user from UT3 editor. Is needed to get the
	 * right order of brushes, because the auto-generated one with ut3.com
	 * batchexport command always mess brush order ...
	 */
	private File intT3dUt3Editor;

	/**
	 * Global logger
	 */
	static final Logger logger = Logger.getLogger("MapConverter");

	/**
	 * Original UT game the map comes from
	 * 
	 * @return
	 */
	public UTGame getInputGame() {
		return inputGame;
	}

	/**
	 * UT game the map will be converted to
	 * 
	 * @return
	 */
	public UTGame getOutputGame() {
		return outputGame;
	}

	/**
	 * Input map that will be converted (Unreal Map (.unr, .ut2) or Unreal Text
	 * Map file (.t3d)
	 * 
	 * @return
	 */
	public File getInMap() {
		return inMap;
	}

	/**
	 * Scale factor applied when converting
	 * 
	 * @return
	 */
	public Double getScale() {
		return scale;
	}

	/**
	 * 
	 * @param inputGame
	 *            Input UT Game
	 * @param outputGame
	 *            Output UT Game
	 */
	public MapConverter(UTGame inputGame, UTGame outputGame) {
		this.inputGame = inputGame;
		this.outputGame = outputGame;
		initialise();
	}

	/**
	 * 
	 * @param inputGame
	 *            Input game the map originally comes from
	 * @param outputGame
	 *            Output game the map will be converted to
	 * @param inpMap
	 *            Map to be converted (either a t3d file or map)
	 * @param path
	 */
	public MapConverter(UTGame inputGame, UTGame outputGame, File inpMap, String path) {
		this.inputGame = inputGame;
		this.outputGame = outputGame;
		this.inMap = inpMap;
		this.outPath = Paths.get(path);
		initialise();
	}

	public MapConverter(UTGame inputGame, UTGame outputGame, File inpMap) {
		this.inputGame = inputGame;
		this.outputGame = outputGame;
		initialise();
	}

	/**
	 * Indicates that gametype is team based
	 * 
	 * @return true is gametype is team based
	 */
	public Boolean isTeamGameType() {
		return isTeamGameType;
	}

	/**
	 *
	 * @param isTeamGameType
	 */
	public void setIsTeamGameType(Boolean isTeamGameType) {
		this.isTeamGameType = isTeamGameType;
	}

	/**
	 * Tried to find property converted to some other game ...
	 * 
	 * @param name
	 * @param withT3dClass
	 * @param properties
	 * @return
	 */
	public T3DMatch.Match getMatchFor(String name, boolean withT3dClass, Map<String, String> properties) {
		return tm.getMatchFor(name, inputGame, outputGame, withT3dClass, properties);
	}

	/**
	 *
	 * @return
	 */
	public HashMap<String, T3DMatch.Match> getActorClassMatch() {
		return tm.getActorClassMatch(inputGame, outputGame);
	}

	private void initOutMapName() {
		if (mapName == null) {
			// TODO being able to set it manually (chosen by user)
			mapName = inMap.getName().split("\\.")[0] + "-" + inputGame.shortName;

			// Remove bad chars from name (e.g: DM-Cybrosis][ -> DM-Cybrosis)
			// else ue4 editor won't be able to set sounds or textures to actors
			mapName = T3DUtils.filterName(mapName);
		}
	}
	
	public void initConvertedResourcesFolder(){
		initOutMapName();
		this.ut4ReferenceBaseFolder = UTGames.UE4_FOLDER_MAP + "/" + this.getOutMapName();
	}
	
	public void setUt4ReferenceBaseFolder (String folder){
		this.ut4ReferenceBaseFolder = folder;
	}
	
	public String getUt4ReferenceBaseFolder (){
		return this.ut4ReferenceBaseFolder;
	}
	
	public File getUt4ReferenceBaseFolderFile (){
		
		// e.g: ut4ReferenceBaseFolder = '/Game/RestrictedAssets/Map/DM-MyMap'
		String xx = ut4ReferenceBaseFolder;
		xx = xx.replace("/Game", "/Content");
		UserGameConfig config = userConfig.getGameConfigByGame(outputGame);
		
		return new File(config.getPath() + File.separator + "UnrealTournament" + File.separator + xx);
	}
	
	private void initialise() {

		if (this.outPath == null && inMap != null) {
			this.outPath = Paths.get(this.getMapConvertFolder().toURI());
		}

		// support for reading targa files
		IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());

		scale = 1d;

		try {

			tm = new T3DMatch(this);

			if (inMap != null) {
				if (inMap.getName().endsWith(".t3d")) {
					inT3d = inMap;
				}

				if (isTeamGameType == null) {
					isTeamGameType = UTGameTypes.isTeamBasedFromMapName(inT3d != null ? inT3d.getName() : inMap.getName());
				}

				getTempExportFolder().mkdirs();

				initConvertedResourcesFolder();
				this.relativeUtMapPath = UTGames.UE4_FOLDER_MAP + "/" + getOutMapName() + "/" + getOutMapName();
			}

			supportedActorClasses = new SupportedClasses(this);

			// Unreal Text map level files (.t3d)
			// do not export full info about textures on polygon but only name
			// (not package)
			// E.g: "Begin Polygon Texture=Ebwl Link=1""
			// so need to load the name to package db
			// not for Unreal 1 no need that since latest Unreal 1 patch
			// from oldunreal.com got better version of unreal engine with full
			// info export
			// E.G: "Begin Polygon Texture=Skaarj.Wall.Ebwl Link=1"
			// TODO test if Unreal 1 path set try export t3d level with Unreal 1
			// for ut99 map
			// so we have always package info at all circumstances
			if (inputGame == UTGame.UT99) {
				loadNameToPackage();
			}

			userConfig = UserConfig.load();
		} catch (JAXBException | IOException ex) {
			Logger.getLogger(MapConverter.class.getName()).log(Level.SEVERE, null, ex);
		}

		// init available extractors
		packageExtractors = new ArrayList<>();
		packageExtractors.add(new UCCExporter(this));
		packageExtractors.add(new CopyExporter(this));
		packageExtractors.add(new SimpleTextureExtractor(this));

		if (userConfig.getUModelPath() != null && userConfig.getUModelPath().exists()) {
			packageExtractors.add(new UModelExporter(this));
		}
	}

	/**
	 * All logs redirect to user interface thought table TODO write log file
	 */
	private void addLoggerHandlers() {

		
		if (conversionViewController == null || conversionViewController.getConvLogTableView() == null) {
			return;
		}

		conversionViewController.getProgressBar().progressProperty().bind(progressProperty());
		conversionViewController.getProgressIndicator().progressProperty().bind(progressProperty());
		conversionViewController.getProgressMessage().textProperty().bind(messageProperty());

		final TableView<TableRowLog> t = conversionViewController.getConvLogTableView();
		t.getItems().clear();

		logger.addHandler(new Handler() {

			@Override
			public void publish(LogRecord record) {
				if (logBuffWriter != null) {
					try {
						logBuffWriter.write(TableRowLog.sdf.format(new Date(record.getMillis())) + " - " + record.getLevel().getName() + " - " + TableRowLog.getMessageFormatted(record) + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				t.getItems().add(new TableRowLog(record));
			}

			@Override
			public void flush() {
				t.getItems().clear();
			}

			@Override
			public void close() throws SecurityException {
				// nothing to do
			}
		});
	}

	final String UPK = "upk";

	/**
	 * Caches all .upk files in UTGame folder of UT3 for performance
	 */
	private void initUt3PackageFilesCache() {

		File utGameFolder = new File(userConfig.getGameConfigByGame(inputGame).getPath().getAbsolutePath() + File.separator + "UTGame");
		packageFilesCache = org.apache.commons.io.FileUtils.listFiles(utGameFolder, new String[] { UPK }, true);

		logger.info("Scanned " + packageFilesCache.size() + " .upk files in UT3 folder");
	}

	/**
	 * Gets file container from .upk file
	 * 
	 * @param packageName
	 *            Package Name (HU_Deco)
	 * @return File container of package (e.g: HU_Deco.upk)
	 */
	public File getUt3PackageFileFromName(String packageName) {

		for (File upk : packageFilesCache) {

			if (upk.getName().toLowerCase().equals(packageName.toLowerCase() + "." + UPK) || upk.getName().toLowerCase().equals(packageName.toLowerCase() + ".ut3")) {
				return upk;
			}
		}

		return null;
	}

	/**
	 * Converts level
	 * 
	 * @throws Exception
	 */
	public void convert() throws Exception {

		
		try {
			
			logFile = new File(outPath.toFile().getAbsolutePath() + File.separator + "conversion.log");
			
			if (logFile != null && logFile.exists()) {
				logFile.delete();
			}
			
			logFile.getParentFile().mkdirs();

			logFileWriter = new FileWriter(logFile);
			logBuffWriter = new BufferedWriter(logFileWriter);
			
			logger.log(Level.INFO, "Writting log file " + logFile);

			logger.log(Level.INFO, "*****************************************");
			logger.log(Level.INFO, "Conversion of " + inMap.getName() + " to " + outputGame.name);
			logger.log(Level.INFO, "Scale Factor: " + scale);
			logger.log(Level.WARNING, "Shader materials are not yet converted");
			logger.log(Level.WARNING, "Terrains are not yet converted");

			if (filteredClasses != null && filteredClasses.length > 0) {
				getSupportedActorClasses().setConvertOnly(filteredClasses);
				logger.log(Level.INFO, "Only these manually specified actor classes will be converted:");

				for (String className : filteredClasses) {
					logger.log(Level.INFO, "-" + className);
				}
			}

			updateProgress(0, 100);

			if (inputGame == UTGame.UT3) {
				initUt3PackageFilesCache();
			}

			if (!outPath.toFile().exists()) {
				outPath.toFile().mkdirs();
			}

			// Export unreal map to Unreal Text map
			if (inT3d == null) {
				updateProgress(10, 100);
				packageFilesCache.add(inMap);
				updateMessage("Exporting map to unreal text file");
				inT3d = UCCExporter.exportLevelToT3d(this, inMap);
				updateProgress(20, 100);
			}

			outT3d = new File(outPath.toFile().getAbsolutePath() + File.separator + mapName + ".t3d");

			// t3d ever exported or directly converting from t3d file, then skip
			// export of it
			// and directly convert it
			t3dLvlConvertor = new T3DLevelConvertor(inT3d, outT3d, this);
			t3dLvlConvertor.setCreateNoteWhenUnconverted(createNoteForUnconvertedActors);
			updateMessage("Converting " + inT3d.getName() + " to " + outT3d.getName());
			t3dLvlConvertor.readConvertAndWrite();
			updateProgress(80, 100);

			// find used textures in staticmeshes
			findTexturesUsedInStaticMeshes();

			cleanAndConvertRessources();

			updateProgress(100, 100);
			updateMessage("All done!");
			logger.log(Level.INFO, "Map was succesfully converted to " + getOutT3d().getAbsolutePath());

			UIUtils.openExplorer(getOutPath().toFile());

			showInstructions();

			writeUnconvertedActorsPropertiesToLogFile();
		} finally {
			if (logBuffWriter != null) {
				logBuffWriter.close();
			}

			if (logFileWriter != null) {
				logFileWriter.close();
			}
		}
	}

	/**
	 * Log to file all unconverted actors and properties of all actors from
	 * original map
	 * 
	 * @throws IOException
	 */
	private void writeUnconvertedActorsPropertiesToLogFile() throws IOException {

		logBuffWriter.write("\n*** Unconverted actors ***\n");

		for (String actorClass : t3dLvlConvertor.getUnconvertedActors()) {
			logBuffWriter.write("- " + actorClass + "\n");
		}

		logBuffWriter.write("\n*** Unconverted properties ***\n");

		for (String actorClass : t3dLvlConvertor.getUnconvertedProperties().keySet()) {
			logBuffWriter.write("*** " + actorClass + " ***\n");

			for (String property : t3dLvlConvertor.getUnconvertedProperties().get(actorClass)) {
				logBuffWriter.write("- " + property + "\n");
			}

			logBuffWriter.write("\n");
		}
	}
	
	/**
	 * Add log info message to guide user converting the map properly TODO add
	 * some pics like in UT3 converter
	 */
	private void showInstructions() {
		logger.log(Level.INFO, "* * * * * * * * I N S T R U C T I O N S * * * * * * * *");
		logger.log(Level.INFO, "Note: these instructions are always under work and might not be 100% accurate");
		logger.log(Level.INFO, "Open Unreal Editor for UT4");
		logger.log(Level.INFO, "File -> New level -> Empty Level");

		final String UE4_MAP_FOLDER = "Content -> RestrictedAssets -> Maps -> WIP -> " + getOutMapName();

		if (hasConvertedRessources()) {
			logger.log(Level.INFO, "Converted Map Ressources Import");
			logger.log(Level.INFO, "In 'Content Browser' panel go to folder: " + UE4_MAP_FOLDER);
			logger.log(Level.INFO, "Click on 'Import' button");

			// TODO beware of terrain textures
			if (convertTextures) {
				logger.log(Level.INFO, "");
				logger.log(Level.INFO, "Textures Import:");
				logger.log(Level.INFO, "Browse for folder : " + getMapConvertFolder().getAbsolutePath() + File.separator + Type.TEXTURE.getName());
				logger.log(Level.INFO, "Select all texture files and press 'Open'");
				logger.log(Level.INFO, "After import, on selected textures right click -> 'Create Material'");
				logger.log(Level.INFO, "Click on 'Save All' in 'Content Browser'");
			}

			if (convertSounds) {
				logger.log(Level.INFO, "");
				logger.log(Level.INFO, "Sounds Import:");
				logger.log(Level.INFO, "Browse for folder : " + getMapConvertFolder().getAbsolutePath() + File.separator + Type.SOUND.getName());
				logger.log(Level.INFO, "Select all sound files and press 'Open'");
				logger.log(Level.INFO, "After import, on selected sounds right click -> 'Create Cue'");
				logger.log(Level.INFO, "Optional: for sound waves make them loop 'Sound wave' -> 'Looping' depending on sound usage (music, ambient sound ..)");
				logger.log(Level.INFO, "Optional: for sound cue used in lifts, add attenuation node with attenuation pattern 'Attenuation_Lifts' ");
				logger.log(Level.INFO, "Click on 'Save All' in 'Content Browser'");
			}

			if (convertStaticMeshes) {
				logger.log(Level.INFO, "");
				logger.log(Level.INFO, "StaticMeshes Import:");
				logger.log(Level.INFO, "Note: some meshes might not be fully textured yet");
				logger.log(Level.INFO, "Manual process is pretty loooong for the moment ... ");
				logger.log(Level.INFO, "Download and install Blender software and open program");
				logger.log(Level.INFO, "Download https://github.com/Befzz/blender3d_import_psk_psa update .psk staticmesh blender addon zip");
				logger.log(Level.INFO, "Update io_import_scene_unreal_psa_psk.py file from zip file in:");
				logger.log(Level.INFO, "C:\\Program Files\\Blender Foundation\\Blender\\<VERSION>\\scripts\\addons folder");
				logger.log(Level.INFO, "Blender | 'File' -> 'User Preferences ...' -> 'Add-ons'");
				logger.log(Level.INFO, "Blender | Check that 'Import-Export: Export Unreal Engine format (.psa/.psk)' add-on is activated");
				logger.log(Level.INFO, "Blender | 'Save user settings' (if you have activated the addon)");
				logger.log(Level.INFO, "Blender | 'File' -> 'New'");
				logger.log(Level.INFO, "Blender | In right panel delete these nodes: 'Cube', 'Lamp', 'Camera' (Right click on each node -> 'Delete hierarchy')");
				logger.log(Level.INFO, "--");
				logger.log(Level.INFO, "For EACH .psk staticmesh in " + getMapConvertFolder().getAbsolutePath() + File.separator + Type.STATICMESH.getName() + " do:");
				logger.log(Level.INFO, "Blender | 'File' -> 'Import' -> 'Skeleton Mesh (.psk)'");
				logger.log(Level.INFO, "Blender | Browse for a .psk staticmesh then 'Import PSK' ");
				logger.log(Level.INFO, "Blender | 'File' -> 'Export' -> 'FBX (.fbx)' ");
				logger.log(Level.INFO, "Blender | Important: keep the same filename with .fbx extension as the original .psk file! ");
				logger.log(Level.INFO, "Blender | Right Click on the 'Orange human icon' (top right) -> 'Delete hierarchy' (for next mesh import) ");
				logger.log(Level.INFO, "--");
				logger.log(Level.INFO, "When all staticmeshes have been converted to .fbx format, go back to Unreal Editor");
				logger.log(Level.INFO, "UE4 | In 'Content Browser' panel go to folder: " + UE4_MAP_FOLDER);
				logger.log(Level.INFO, "UE4 | Click on 'Import' and browse for your all .fbx staticmesh");
				logger.log(Level.INFO, "UE4 | In 'FBX Import Options' set Transform -> Import uniform scale to 0.01 ");
				logger.log(Level.INFO, "UE4 | In 'FBX Import Options' set Material -> Import Material to false ");
				logger.log(Level.INFO, "UE4 | In 'FBX Import Options' set Material -> Import Textures to true ");
				logger.log(Level.INFO, "UE4 | Click on 'Import'");

			}

			logger.log(Level.INFO, "* * * * ");
		}

		logger.log(Level.INFO, "Converted Map Import");
		logger.log(Level.INFO, "Open the .t3d file " + getOutT3d() + " with a text editor.");
		logger.log(Level.INFO, "Select all text into .t3d file and copy/paste it into main viewport of the editor.");
		logger.log(Level.INFO, "'Save' the map");

		if (isFrom(UnrealEngine.UE1, UnrealEngine.UE2)) {
			logger.log(Level.INFO, "Optional: delete the 'Sky Brush' area and resize the " + T3DLevelConvertor.LIGHTMASS_IMP_VOL_NAME + " to fit with level");
			logger.log(Level.INFO, "Optional: delete the 'Sky Brush' area and resize the " + T3DLevelConvertor.THE_BIG_BRUSH_NAME + " to fit with level");
		}
		logger.log(Level.INFO, "Build map");
	}

	/**
	 * 
	 * @return
	 */
	private boolean hasConvertedRessources() {
		return convertMusic || convertSounds || convertStaticMeshes || convertTextures;
	}

	/**
	 * Flag textures in staticmeshes used in map as used Display material info
	 * in logs as well
	 * 
	 * TODO re-write generated psk files to change material name to fit with
	 * packname_group_name convention (with max length = 64)
	 */
	private void findTexturesUsedInStaticMeshes() {

		PSKStaticMesh pskMesh = null;

		for (UPackage unrealPackage : mapPackages.values()) {

			for (UPackageRessource ressource : unrealPackage.getRessources()) {

				if (!ressource.isUsedInMap() || ressource.getType() != Type.STATICMESH) {
					continue;
				}

				// .psk file
				File exportedFile = ressource.getExportedFile();

				if (exportedFile != null && exportedFile.length() > 0) {
					try {
						pskMesh = new PSKStaticMesh(exportedFile);
						String mats = "";

						for (Material mat : pskMesh.getMaterials()) {

							String matName = mat.getMaterialName();

							if (matName != null) {
								UPackageRessource matR = findRessourceByNameOnly(matName, Type.TEXTURE);

								if (matR != null) {

									// try replace shader with diffuse texture
									if (matR.getMaterialInfo() != null) {
										matR.getMaterialInfo().findRessourcesFromNames(this);

										if (matR.getMaterialInfo().getDiffuse() != null) {
											UPackageRessource diffuse = matR.getMaterialInfo().getDiffuse();
											matR.getMaterialInfo().setIsUsedInMap(true);

											if (diffuse != null) {
												matR.replaceWith(diffuse);
												diffuse.setUsedInStaticMesh(true);
											}
										}
									}

									matR.setIsUsedInMap(true);
									matR.setUsedInStaticMesh(true);
									// change original material name: "<name>"
									// to <packagename>_<group>_<name>_mat
									// or <packagename>_<name>_mat
									// or <name>_mat to fit with max size of 64
									// byte for material name
									mat.setMaterialName(matR.getConvertedBaseName(this));
									mats += matR.getFullName();
								} else {
									mats += matName;
								}

								mats += " | ";
							}
						}

						// re-write .psk file generated by umodel so material
						// name
						// fit with <packagename>_<group>_<name>_mat convention
						File tempPskDir = new File(exportedFile.getParent() + File.separator + "NEWPSK");

						if (!tempPskDir.exists()) {
							tempPskDir.mkdirs();
						}

						File newPskFile = new File(tempPskDir.getAbsolutePath() + File.separator + exportedFile.getName());
						pskMesh.write(newPskFile);
						ressource.setExportedFile(newPskFile);

						// logger.log(Level.INFO, "Material used for " +
						// ressource);
						// logger.log(Level.INFO, mats);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error while reading file " + exportedFile, e);
					}
				}
			}
		}
	}

	/**
	 * Delete unused files and convert them to good format if needed. (e.g:
	 * convert staticmeshes to .ase or .fbx format for import in UE4)
	 * 
	 * @throws IOException
	 */
	private void cleanAndConvertRessources() throws IOException {

		updateMessage("Converting ressource files");
		boolean wasConverted;

		// remove unecessary exported files
		// convert them to some new file format if needed
		// and rename them to fit with "naming" standards
		for (UPackage unrealPackage : mapPackages.values()) {

			for (UPackageRessource ressource : unrealPackage.getRessources()) {

				try {

					wasConverted = false;
					File exportedFile = ressource.getExportedFile();

					if (exportedFile != null && exportedFile.length() > 0) {

						// Renaming exported files (e.g: Stream2.wav ->
						// AmbOutside_Looping_Stream2.wav)
						// move them to non temporary folder
						if (ressource.isUsedInMap()) {

							// Some sounds and/or textures might need to be
							// converted for correct import in UE4
							if (ressource.needsConversion(this)) {
								exportedFile = ressource.convert(logger, userConfig);
								ressource.setExportedFile(exportedFile);
								wasConverted = true;
							}

							File newFile = new File(getMapConvertFolder().getAbsolutePath() + File.separator + ressource.getType().getName() + File.separator + ressource.getConvertedFileName());
							newFile.getParentFile().mkdirs();
							newFile.createNewFile();

							// sometimes it does not find the exported texture
							// (?
							// ... weird)
							if (exportedFile.exists() && exportedFile.isFile()) {
								Files.copy(exportedFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							}

							if (exportedFile.delete()) {
								logger.fine("Deleted " + exportedFile);
							}

							exportedFile = newFile;
							ressource.setExportedFile(exportedFile);

							if (wasConverted) {
								logger.fine("Converted " + ressource.getType().name() + " :" + newFile.getName());
							}
						}
					}

				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}

		// always keep original .t3d file
		try {
			Files.move(inT3d.toPath(), new File(getOutPath().toString() + File.separator + "myLevel_unconverted.t3d").toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			logger.warning(e.getReason());
		}

		updateMessage("Deleting temporary files");

		org.apache.commons.io.FileUtils.deleteQuietly(getTempExportFolder());

		// Create a folder for this map in UE4Editor
		// and copy a simple existing .uasset file so we can see the folder
		// created in UT4 editor ...
		if (toUT4()) {
			UserGameConfig userGameConfig = userConfig.getGameConfigByGame(UTGame.UT4);

			if (userGameConfig.getPath() == null || !userGameConfig.getPath().exists()) {
				logger.log(Level.WARNING, "UT4 Editor path not set in settings!");
				return;
			}

			File restrictedAssetsFolder = new File(userGameConfig.getPath() + File.separator + "UnrealTournament" + File.separator + "Content" + File.separator + "RestrictedAssets");
			// TEMP thingy use custom one if user changed it
			//File wipConvertedMapFolder = new File(UTGames.getMapsFolder(userGameConfig.getPath(), outputGame) + File.separator + getOutMapName());
			File wipConvertedMapFolder = getUt4ReferenceBaseFolderFile();
			
			wipConvertedMapFolder.mkdirs();

			logger.log(Level.FINE, "Creating " + wipConvertedMapFolder);

			// copy small .uasset file so the folder will appear in UT4 editor
			// ....
			File uassetFile = new File(restrictedAssetsFolder + File.separator + "Blueprints" + File.separator + "Lift" + File.separator + "Curves" + File.separator + "EaseIn-Out.uasset");
			File uassetCopy = new File(wipConvertedMapFolder + File.separator + "dummyfile.uasset");

			if (!uassetCopy.exists()) {
				Files.copy(uassetFile.toPath(), uassetCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 *
	 * @return
	 */
	public SupportedClasses getSupportedActorClasses() {
		return supportedActorClasses;
	}

	/**
	 *
	 * @return
	 */
	public T3DLevelConvertor getT3dLvlConvertor() {
		return t3dLvlConvertor;
	}

	/**
	 *
	 * @param t3dLvlConvertor
	 */
	public void setT3dLvlConvertor(T3DLevelConvertor t3dLvlConvertor) {
		this.t3dLvlConvertor = t3dLvlConvertor;
	}

	public String getOutMapName() {
		return mapName;
	}

	/**
	 * Current user configuration such as program path for UT99 and so on ...
	 * 
	 * @return
	 */
	public UserConfig getUserConfig() {
		return userConfig;
	}

	/**
	 * 
	 * @return <code>true</code> if the output game is using unreal engine 4
	 */
	public boolean toUnrealEngine4() {
		return UTGames.isUnrealEngine4(this.getOutputGame());
	}

	/**
	 *
	 * @return
	 */
	public UTGames.UnrealEngine getUnrealEngineTo() {
		return this.getOutputGame().engine;
	}

	/**
	 * Indicated if converting from UT using Unreal Engine 1 or Unreal Engine 2
	 * (basically Unreal1, UT99, Unreal 2, UT2003 and UT2004)
	 * 
	 * @return true if converting from Unreal Engine 1 or 2 UTx game
	 * @deprecated Use isFrom
	 */
	@Deprecated
	public boolean fromUE1OrUE2() {
		return UTGames.isUnrealEngine1(this.getInputGame()) || UTGames.isUnrealEngine2(this.getInputGame());
	}

	public boolean isFrom(UnrealEngine... engines) {

		if (engines.length == 0) {
			return false;
		}

		for (UnrealEngine engine : engines) {
			if (engine == this.getInputGame().engine) {
				return true;
			}
		}

		return false;
	}

	public boolean isTo(UnrealEngine... engines) {

		if (engines.length == 0) {
			return false;
		}

		for (UnrealEngine engine : engines) {
			if (engine == this.getOutputGame().engine) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Indicated if converting to UT using Unreal Engine 1 or Unreal Engine 2
	 * (basically Unreal1, UT99, Unreal 2, UT2003 and UT2004)
	 * 
	 * @return true if converting to Unreal Engine 1 or 2 UTx game
	 * @deprecated Use isFrom
	 */
	@Deprecated
	public boolean toUE1OrUE2() {
		return UTGames.isUnrealEngine1(this.getOutputGame()) || UTGames.isUnrealEngine2(this.getOutputGame());
	}

	/**
	 * Indicates games being converted from some UT game in unreal engine 3 or 4
	 * 
	 * @return
	 */
	public boolean fromUE1orUE2OrUE3() {
		return UTGames.isUnrealEngine1(this.getInputGame()) || UTGames.isUnrealEngine2(this.getInputGame()) || UTGames.isUnrealEngine3(this.getInputGame());
	}

	/**
	 * Indicates games being converted from some UT game in unreal engine 3 or 4
	 * 
	 * @return
	 */
	public boolean fromUE3OrUE4() {
		return UTGames.isUnrealEngine3(this.getInputGame()) || UTGames.isUnrealEngine4(this.getInputGame());
	}

	/**
	 * Indicates games being converted to some UT game in unreal engine 3 or 4
	 * 
	 * @return
	 */
	public boolean toUE3OrUE4() {
		return UTGames.isUnrealEngine3(this.getOutputGame()) || UTGames.isUnrealEngine4(this.getOutputGame());
	}

	/**
	 * Indicated game being converted to Unreal Engine 3 game (basically only
	 * UT3)
	 * 
	 * @return
	 * @deprecated Use isTo
	 */
	@Deprecated
	public boolean toUE3() {
		return UTGames.isUnrealEngine3(this.getOutputGame());
	}

	/**
	 * Tells if converting UT game using Unreal Engine 1 or 2 is being converted
	 * to some other UT game using Unreal Engine 3 or 4.
	 * 
	 * @return true if converting UT game using Unreal Engine 1 or 2 to UT game
	 *         using Unreal Engine 3 or 4
	 */
	public boolean isFromUE1UE2ToUE3UE4() {
		return fromUE1OrUE2() && toUE3OrUE4();
	}

	public boolean fromUE123ToUE4() {
		return fromUE1orUE2OrUE3() && toUnrealEngine4();
	}

	public boolean toUT4() {
		return outputGame == UTGame.UT4;
	}

	/**
	 * @deprecated Use isTo
	 * @return
	 */
	@Deprecated
	public boolean toUE4() {
		return UTGames.isUnrealEngine4(this.getOutputGame());
	}

	/**
	 * Tells if converting UT game using Unreal Engine 1 or 2 is being converted
	 * to some other UT game using Unreal Engine 3 or 4.
	 * 
	 * @return true if converting UT game using Unreal Engine 1 or 2 to UT game
	 *         using Unreal Engine 3 or 4
	 */
	public boolean isFromUE3UE4ToUE1UE2() {
		return toUE1OrUE2() && fromUE3OrUE4();
	}

	public File getOutT3d() {
		return outT3d;
	}

	public void setConversionViewController(ConversionViewController conversionViewController) {
		this.conversionViewController = conversionViewController;
		addLoggerHandlers();
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * <UT4ConverterFolder>/Converted
	 * 
	 * @return
	 */
	private static File getBaseConvertFolder() {
		return new File(Installation.getProgramFolder().getAbsolutePath() + File.separator + CONV_PATH);
	}

	/**
	 * <UT4ConverterFolder>/Converted/<MapName>
	 * 
	 * @return
	 */
	public File getMapConvertFolder() {
		return new File(getBaseConvertFolder() + File.separator + getInMap().getName().split("\\.")[0]);
	}

	/**
	 * <UT4ConverterFolder>/Converted/<MapName>/Temp
	 * 
	 * @return
	 */
	public File getTempExportFolder() {
		return new File(getMapConvertFolder() + File.separator + "Temp");
	}

	Map<String, String> nameToPackage = new HashMap<>();

	private void loadNameToPackage() throws FileNotFoundException, IOException {

		File dbFile = new File(Installation.getProgramFolder() + File.separator + "conf" + File.separator + inputGame.shortName + "TexNameToPackage.txt");

		if (dbFile.exists()) {
			try (FileReader fr = new FileReader(dbFile); BufferedReader bfr = new BufferedReader(fr);) {

				String line;

				while ((line = bfr.readLine()) != null) {
					String[] sp = line.split("\\:");
					nameToPackage.put(sp[0], sp[1]);
				}
			}
		} else {
			logger.log(Level.WARNING, "Texture db file " + dbFile + " not found !");
		}
	}

	/**
	 * Find package ressource by simple name
	 * 
	 * @param name
	 *            Simple name (e.g: 'bas05bHA')
	 * @param resType
	 *            Ressource type
	 * @return Ressource using that name
	 */
	public UPackageRessource findRessourceByNameOnly(String name, Type resType) {

		UPackageRessource ressource = null;
		name = name.toLowerCase();

		for (UPackage pack : mapPackages.values()) {

			for (UPackageRessource pakRes : pack.getRessources()) {
				if (pakRes.getType() == resType && pakRes.getName().toLowerCase().equals(name)) {
					ressource = pakRes;
					break;
				}
			}
		}

		return ressource;
	}

	public UPackageRessource getUPackageRessource(String fullRessourceName, T3DRessource.Type type) {
		return getUPackageRessource(fullRessourceName, null, type);
	}

	/**
	 * T3D actor properties which are ressources (basically sounds, music,
	 * textures, ...)
	 * 
	 * @param fullRessourceName
	 *            Full name of ressource (e.g: AmbModern.Looping.comp1 )
	 * @param type
	 *            Type of ressource (sound, staticmesh, texture, ...)
	 * @param packageName
	 *            Package Name (e.g: "AmbModern"). Null if don't have this info
	 *            yet
	 * @return
	 */
	public UPackageRessource getUPackageRessource(String fullRessourceName, String packageName, T3DRessource.Type type) {

		if (fullRessourceName == null) {
			return null;
		}

		String[] split = fullRessourceName.split("\\.");

		// no package info
		if (packageName == null) {

			// having only name of ressource not which package it belongs to
			// happens for UE1/2 where polygon t3d data only store name
			// so we using the old "ut3 converter" name to package db until
			// finding a better way ...
			if (split.length <= 1) {

				// for ut99 polygon data does not give package info
				if (type == T3DRessource.Type.TEXTURE && inputGame == UTGame.UT99) {
					String name = split[0];
					packageName = nameToPackage.get(name.toLowerCase());
					fullRessourceName = packageName + "." + name;

				}
				// assuming it's from map
				// as seen in CTF-Turbo for staticmeshes from map package
				// does not give full ressource name
				// e.g: StaticMesh=StaticMesh'sm_Lamp_02'
				// but ressource in "CTF-Turbo.StaticMeshes_Lamps.sm_Lamp_02"
				// ...
				// FIXME on exporting ressource get the right group
				else if (isFrom(UnrealEngine.UE3)) {
					packageName = getMapPackageName();
					fullRessourceName = packageName + "." + split[0];
				}
			} else {
				packageName = fullRessourceName.split("\\.")[0];
			}
		} else {
			if (!fullRessourceName.contains(".")) {

				try {
					fullRessourceName = packageName + "." + fullRessourceName;
				} catch (Exception e) {
					throw e;
				}
			}
		}
		if (packageName != null && "mylevel".equals(packageName.toLowerCase())) {
			packageName = getMapPackageName();
		}

		// Ressource ever created while parsing previous t3d lines
		// we return it
		if (mapPackages.containsKey(packageName)) {

			UPackage unrealPackage = mapPackages.get(packageName);
			UPackageRessource uPackageRessource = unrealPackage.findRessource(fullRessourceName);

			if (uPackageRessource != null) {
				uPackageRessource.setIsUsedInMap(true);
				return uPackageRessource;
			}
			// Need to create one
			else {
				return new UPackageRessource(this, fullRessourceName, type, unrealPackage, true);
			}
		}

		else {

			// need to create one (unreal package info is auto-created)
			UPackageRessource upRessource = new UPackageRessource(this, fullRessourceName, type, true);
			mapPackages.put(packageName, upRessource.getUnrealPackage());
			return upRessource;
		}
	}

	/**
	 * Force converter not to convert any binary ressources
	 */
	public void noConvertRessources() {
		this.convertSounds = false;
		this.convertStaticMeshes = false;
		this.convertTextures = false;
		this.convertMusic = false;
	}

	@Override
	protected T3DLevelConvertor call() throws Exception {
		try {
			convert();
		} catch (Exception e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
		return t3dLvlConvertor;
	}

	public void setScale(Double scale) {
		this.scale = scale;
	}

	public ConversionViewController getConversionViewController() {
		return conversionViewController;
	}

	public void setInMap(File inMap) {
		this.inMap = inMap;
		this.outPath = null;
		packageExtractors.clear();
		this.mapName = null;
		initialise();
	}

	public Path getOutPath() {
		return outPath;
	}

	public boolean convertTextures() {
		return convertTextures;
	}

	/**
	 * Says if program can convert/export textures. For unreal engine <= 2 we
	 * can still user the stock "ucc.exe" one except for Unreal 2 which always
	 * produces "0" bytes file size but for UUE3/4 need umodel program
	 * 
	 * @return <code>true</code> if it's possible to convert texture otherwise
	 *         false
	 */
	public boolean canConvertTextures() {

		if (userConfig.getUModelPath() != null && userConfig.getUModelPath().exists()) {
			return true;
		}

		// todo handle/test ucc_bin for Unreal 1 for linux with
		// www.oldunreal.com patch
		if (inputGame.engine.version <= UnrealEngine.UE2.version && inputGame != UTGame.U2) {
			return userConfig.hasGamePathSet(inputGame) && Installation.isWindows();
		}

		return false;
	}

	/**
	 * Says if program can convert/export sounds.
	 * 
	 * @return
	 */
	public boolean canConvertSounds() {

		if (userConfig.getUModelPath() != null && userConfig.getUModelPath().exists()) {
			return true;
		}

		if (inputGame.engine.version <= UnrealEngine.UE2.version) {
			// todo handle/test ucc_bin for Unreal 1 for linux with
			// www.oldunreal.com patch
			return userConfig.hasGamePathSet(inputGame) && Installation.isWindows();
		}

		return false;
	}

	public boolean canConvertMusic() {
		// just a file copy for UT2004 (.ogg files ...)
		if (inputGame.engine == UnrealEngine.UE2) {
			return true;
		} else {
			return canConvertSounds();
		}
	}

	public boolean convertSounds() {
		return convertSounds;
	}

	public boolean convertStaticMeshes() {
		return convertStaticMeshes;
	}

	/**
	 * Says if program can export staticmeshes
	 * 
	 * @return
	 */
	public boolean canConvertStaticMeshes() {

		// no staticmeshes for UE1 (UT99 + Unreal 1)
		if (inputGame.engine == UnrealEngine.UE1) {
			return false;
		}

		// note: can export with ucc for UT2003/UT2004 but .t3d mesh format
		// not working for import with UE4
		// conversion only partial for umodel
		// since pskx cannot be used by UE4 but can be imported with blender
		// and exported to fbx or obj format file allowed by UE4
		return userConfig.getUModelPath() != null && userConfig.getUModelPath().exists();
	}

	public boolean convertMusic() {
		return convertMusic;
	}

	public void setConvertTextures(boolean convertTextures) {
		this.convertTextures = convertTextures;
	}

	public void setConvertSounds(boolean convertSounds) {
		this.convertSounds = convertSounds;
	}

	public void setConvertStaticMeshes(boolean convertStaticMeshes) {
		this.convertStaticMeshes = convertStaticMeshes;
	}

	public void setConvertMusic(boolean convertMusic) {
		this.convertMusic = convertMusic;
	}

	public void setOutMapName(String outMapName) {
		this.mapName = outMapName;
	}

	/**
	 * relativeUtMapPath
	 * 
	 * @return /Game/Maps/<mapname>/<mapname>
	 */
	public String getRelativeUtMapPath() {
		return relativeUtMapPath;
	}

	/**
	 * From map file return map package name E.g: DM-Ranking.ut2 -> DM-Ranking
	 * 
	 * @return
	 */
	public String getMapPackageName() {
		return getInMap().getName().split("\\.")[0];
	}

	/**
	 * Find package used in map by name
	 * 
	 * @param name
	 *            Name of package (not case sensitive)
	 * @return
	 */
	public UPackage findPackageByName(String name) {

		UPackage pack = null;

		if (name != null && mapPackages.containsKey(name)) {
			pack = mapPackages.get(name);
		}

		return pack;
	}

	public Map<String, UPackage> getMapPackages() {
		return mapPackages;
	}

	/**
	 * If true for each unconverted actor will create a "Note" actor in
	 * converted level
	 * 
	 * @param createNoteForUnconvertedActors
	 *            <code>true</code> Will create note for unconverted actors else
	 *            no
	 */
	public void setCreateNoteForUnconvertedActors(boolean createNoteForUnconvertedActors) {
		this.createNoteForUnconvertedActors = createNoteForUnconvertedActors;
	}

	public File getIntT3dUt3Editor() {
		return intT3dUt3Editor;
	}

	public void setIntT3dUt3Editor(File intT3dUt3Editor) {
		this.intT3dUt3Editor = intT3dUt3Editor;
	}

	/**
	 * Return ut class filter during conversion if any.
	 * 
	 * @return Filtered classes
	 */
	public String[] getFilteredClasses() {
		return filteredClasses;
	}

	/**
	 * Set a filter to UT classes that will be converted. If no filter is set
	 * then all classes that can be converted will be converted.
	 * 
	 * @param filteredClasses
	 *            Classes that will be converted (e.g:
	 *            ['Brush','Light','DefensePoint', ...]
	 */
	public void setFilteredClasses(String[] filteredClasses) {
		this.filteredClasses = filteredClasses;
	}

	public boolean hasClassFilter() {
		return filteredClasses != null && filteredClasses.length > 0;
	}
}
