package com.baidu.duer.dcs.guo_imagload.wx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DiskLruCache {

	//非android sdk
	public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
			throws IOException {
		//
		return new DiskLruCache();
	}
	public Editor edit(String key) {
		return new Editor();
	}
	public class Snapshot {

		public FileInputStream getInputStream(int i) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	public class Editor {

		public OutputStream newOutputStream(int i) {
			// TODO Auto-generated method stub
			return null;
		}

		public void commit() {
			// TODO Auto-generated method stub

		}

		public void abort() {
			// TODO Auto-generated method stub

		}

	}

	public void flush() {
		// TODO Auto-generated method stub

	}
	public Snapshot get(String key) {
		return new Snapshot();
	}
}
