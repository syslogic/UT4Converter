package org.xtx.ut4converter.tools.vertmesh;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.xtx.ut4converter.tools.BinUtils;
import org.xtx.ut4converter.tools.psk.PSKStaticMesh.BinReadWrite;

public class FJSMeshTri implements BinReadWrite {

	/**
	 * _WORD iVertex[3]; // Vertex indices. = Short
	 * BYTE Type; // James' mesh type.(unused) 
	 * BYTE Color; // Color for flat and Gouraud shaded. (unused)
	 * FMeshByteUV Tex[3]; // Texture UV coordinates. 
	 * BYTE TextureNum; // Source texture offset. 
	 * BYTE Flags; // Unreal mesh flags (currently unused).
	 */

	short iVertex[]; // 3
	byte type;
	byte color;
	FMeshByteUV[] tex; // 3
	byte textureNum;
	byte flags;

	/**
	 * Texture coordinates associated with a vertex and one or more mesh
	 * triangles. All triangles sharing a vertex do not necessarily have the
	 * same texture coordinates at the vertex.
	 *
	 */
	class FMeshByteUV {
		byte u;
		byte v;
	};

	@Override
	public void write(FileOutputStream bos) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void read(ByteBuffer bf) {
		iVertex = BinUtils.readShortArray(bf, 3);
		type = bf.get();
		color = bf.get();
		FMeshByteUV tex_0 = new FMeshByteUV();
		tex_0.u = bf.get();
		tex_0.v = bf.get();

		FMeshByteUV tex_1 = new FMeshByteUV();
		tex_1.u = bf.get();
		tex_1.v = bf.get();

		FMeshByteUV tex_2 = new FMeshByteUV();
		tex_2.u = bf.get();
		tex_2.v = bf.get();

		tex = new FMeshByteUV[3];
		tex[0] = tex_0;
		tex[1] = tex_1;
		tex[2] = tex_2;

		textureNum = bf.get();
		flags = bf.get();
	}
	
	public String toString() {
		String s = "";
		s += "iVertex: " + iVertex[0] + ", " + iVertex[1] + "," + iVertex[1] + "\n";
		s += "type: " + type + "\n";
		s += "color: " + color + "\n";
		
		for(int i=0; i < tex.length; i++){
			s += "tex: " + tex[i].u + ", " + tex[i].v  + "\n";
		}
		
		
		s += "textureNum: " + textureNum + "\n";
		s += "flags: " + flags + "\n";

		return s;
	}

}
