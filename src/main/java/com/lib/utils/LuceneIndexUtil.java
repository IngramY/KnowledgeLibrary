package com.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import com.hankcs.hanlp.HanLP;
import com.hankcs.lucene.HanLPAnalyzer;
import com.hankcs.lucene.HanLPTokenizer;
import com.lib.entity.FileInfo;
import com.lib.enums.Const;


/**
 * 创建索引 Lucene 5.0+
 * 
 * @author Administrator
 * 
 */
public class LuceneIndexUtil { 
	
	//索引存放路径
	private static String indexPath=Const.ROOT_PATH+"lucene";
	// 词法分析器
	private static Analyzer analyzer = new HanLPAnalyzer() {
		@Override
		protected TokenStreamComponents createComponents(String arg0) {
			Tokenizer tokenizer = new HanLPTokenizer(
					HanLP.newSegment().enableIndexMode(true).enableJapaneseNameRecognize(true).enableIndexMode(true)
							.enableNameRecognize(true).enablePlaceRecognize(true),
					null, false);
			return new TokenStreamComponents(tokenizer);
		}
	};
	/**
	 * 添加文件索引
	 * @param file
	 */
	public synchronized static void addFileIndex(FileInfo file,String fileUserName) {
		Document document = new Document();
		// 创建Directory对象
		IndexWriter indexWriter = null;
		// 创建Directory对象
		Directory directory =null;
		// 创建IndexWriter对象,
		IndexWriterConfig config=null;
		//pdf文件存放路径
	    String pdfPath=Const.ROOT_PATH + file.getFilePath() + ".pdf";
		try {
			
			directory = FSDirectory.open(new File(indexPath).toPath());
			config = new IndexWriterConfig(analyzer);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			indexWriter = new IndexWriter(directory, config);
			// 判断是否需要建文档内容索引
			if(new File(pdfPath).exists()){
				// 创建输入流读取pdf文件
				String result = "";
				FileInputStream is = null;
				PDDocument PDdoc = null;
				try {
					is = new FileInputStream(new File(pdfPath));
					PDFParser parser = new PDFParser(is);
					parser.parse();
					PDdoc = parser.getPDDocument();
					PDFTextStripper stripper = new PDFTextStripper();
					result = stripper.getText(PDdoc);
					if(result!=""){
			
					document.add(new TextField("fileText", result, Field.Store.YES));
					List<String> fileKeyWords = HanLP.extractKeyword(result, 10);
					
					List<String>  fileSummarys =HanLP.extractSummary(result, 10);
					
					document.add(new TextField("fileKeyWords",fileKeyWords.toString(), Field.Store.YES));
					
					document.add(new StringField("fileSummarys",fileSummarys.toString(), Field.Store.YES));
					
					
					}
				} catch (Exception e) {
					close(indexWriter,directory);
					e.printStackTrace();
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (PDdoc != null) {
						try {
							PDdoc.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				}
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// System.out.println(doc.getDocModTime()+" "+doc.getDocUpTime());
			
			document.add(new StringField("fileUserName", fileUserName, Field.Store.YES));
			
			document.add(new StringField("fileUuid", file.getFileUuid()+"", Field.Store.YES));
			
			document.add(new TextField("fileName", file.getFileName()+"", Field.Store.YES));
			
			document.add(new StringField("fileExt", file.getFileExt() + "", Field.Store.YES));
			
			document.add(new TextField("fileBrief", file.getFileBrief() + "", Field.Store.YES));
			
			document.add(new StringField("fileUserId", file.getFileUserId()+"", Field.Store.YES));
			
			document.add(new StringField("fileCreateTime", sdf.format(file.getFileCreateTime()), Field.Store.YES));
			
			document.add(new StringField("filePath", file.getFilePath()+"", Field.Store.YES));
			
			document.add(new StringField("fileState", file.getFileState() + "", Field.Store.YES));
			
			document.add(new StringField("fileClassId", file.getFileClassId() + "", Field.Store.YES));
			
			indexWriter.addDocument(document);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(indexWriter,directory);
		}
	}

	/**
	 * 
	 * 根据id删除file索引
	 * @param doc
	 * @throws IOException
	 */
	public static void deteleFileIndex(FileInfo file){
		
		// 创建Directory对象
		IndexWriter indexWriter = null;
		// 创建Directory对象
		Directory directory =null;
		// 创建IndexWriter对象,
		IndexWriterConfig config=null;
		try{
		directory = FSDirectory.open(new File(indexPath).toPath());
		config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
	    indexWriter = new IndexWriter(directory, config);
		indexWriter.deleteDocuments(new Term("fileId", file.getFileId() + ""));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(indexWriter,directory);
		}
	}
	
	public static void close(IndexWriter indexWriter,Directory directory)
	{
		try {
			if (indexWriter != null) {
				indexWriter.commit();
				indexWriter.close();
			}
			if (directory != null) {
				directory.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
