package org.xtx.ut4converter.tools.vertmesh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


/**
 * Class for reading vertmesh files (.3d files)
 * 
 * @author XtremeXp
 *
 */
public class VertMesh {

	/**
	 * Order how bytes are read for vert mesh files
	 */
	private static final ByteOrder BYTE_ORDER_LE = ByteOrder.LITTLE_ENDIAN;
	private File vertMeshFile;
	private FJSDataHeader dataHead;
	private List<FJSMeshTri> faces;

	public VertMesh() {
		init();
	}

	public VertMesh(File vertMeshFile) {
		this.vertMeshFile = vertMeshFile;
		init();
	}

	private void init() {
		dataHead = new FJSDataHeader();
		faces = new ArrayList<>();
	}

	
	
	public FJSDataHeader getDataHead() {
		return dataHead;
	}

	public void read() throws Exception {

		if (vertMeshFile == null || !vertMeshFile.exists()) {
			return;
		}

		FileChannel inChannel = null;

		ByteBuffer buffer = null;
		
		try (FileInputStream fis = new FileInputStream(vertMeshFile)) {

			inChannel = fis.getChannel();
			buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) vertMeshFile.length());
			buffer.order(BYTE_ORDER_LE);
			dataHead.read(buffer);
			
			for(int i=0; i < dataHead.getNumPolys() ; i++){
				FJSMeshTri face = new FJSMeshTri();
				face.read(buffer);
				faces.add(face);
			}
		}
		finally {
			try {
				inChannel.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {

		File f = new File("C:\\Temp\\umodel_win32\\UmodelExport\\UnrealShare\\VertMesh\\Female1_d.3d");

		VertMesh vm = new VertMesh(f);
		try {
			System.out.println("Reading ...");
			vm.read();
			System.out.println(vm.getDataHead());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
