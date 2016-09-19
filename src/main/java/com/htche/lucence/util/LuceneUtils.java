package com.htche.lucence.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.htche.lucence.services.LuceneService;

/**
 * Lucene工具类
 * @author zhaizhiyi
 * 2016年9月12日
 *
 */
public class LuceneUtils<T> {

	private static final LuceneService luceneService = LuceneService.getInstance();
	public static Analyzer analyzer = new IKAnalyzer();
	String[]multiFields=null;
	static String writePath;
	String[] readPatharr;
	static String readPaths;
	
	static {
		Properties p = new Properties();
		try {
			p.load(LuceneUtils.class.getClassLoader().getResourceAsStream("application.properties"));
			readPaths = p.getProperty("lucene.readpath");
			writePath= p.getProperty("lucene.writepath");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Directory getDirectory()
	{
		Directory directory=null;
		try {
			File file=new File(writePath);
			directory = FSDirectory.open(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return directory;
	}
	
	public Query getMultiQuery(String keyword,Class cls)
	{
		java.lang.reflect.Field[] fields=cls.getDeclaredFields();
		String[]multiFields=new String[fields.length];
		for(int i=0;i<fields.length;i++)
		{
			multiFields[i]=fields[i].getName();
		}
		this.multiFields=multiFields;
		MultiFieldQueryParser parser=new MultiFieldQueryParser(Version.LUCENE_47,multiFields,analyzer);
		Query query=null;
		try 
		{
			if(null==keyword||keyword.equals(""))
			{
				query=new MatchAllDocsQuery();
			}
			else
			{
				query=parser.parse(keyword);
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return query;
	}
	
	public IndexReader getMultiReader(String luceneDir)
	{
		String[] dir=luceneDir.split(";");
		return getMultiReader(dir);
	}
	
	/**
	 * 多索引目录查询  
	 * @param luceneDir
	 * @return
	 */
	public IndexReader getMultiReader(String[] luceneDir) 
	{
		int length = luceneDir.length;
		IndexReader[] reader = new IndexReader[length];
		try 
		{
			for (int i = 0; i < length; i++) 
			{
				reader[i] = DirectoryReader.open(openFSDirectory(luceneDir[i]));
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		MultiReader multiReader = new MultiReader(reader);
		return multiReader;
	}
	  /**
	   * 打开索引目录
	   * 
	   * @param luceneDir
	   * @return
	   * @throws IOException
	   */
	  public static FSDirectory openFSDirectory(String luceneDir) {
	    FSDirectory directory = null;
	    try {
	      directory = FSDirectory.open(new File(luceneDir));
	      /**
	       * 注意：isLocked方法内部会试图去获取Lock,如果获取到Lock，会关闭它，否则return false表示索引目录没有被锁，
	       * 这也就是为什么unlock方法被从IndexWriter类中移除的原因
	       */
	      IndexWriter.isLocked(directory);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return directory;
	  }
	  
	  /**
	   * 关闭索引目录并销毁
	   * @param directory
	   * @throws IOException
	   */
	  public static void closeDirectory(Directory directory) throws IOException {
	    if (null != directory) {
	      directory.close();
	      directory = null;
	    }
	  }
	  /**
	   * 获取 IndexWriterConfig
	   * @return
	   */
	  public static IndexWriterConfig getIndexWriterConfig()
	  {
		  IndexWriterConfig config=new IndexWriterConfig(Version.LUCENE_47,analyzer);
		  return config;
	  }
	  /**
	   * 获取IndexWriter
	   * @param dir
	   * @param config
	   * @return
	   */
	  public static IndexWriter getIndexWrtier(Directory dir, IndexWriterConfig config) {
	    return luceneService.getIndexWriter(dir, config);
	  }
	  
	  /**
	   * 获取IndexWriter
	   * @param dir
	   * @param config
	   * @return
	   */
	  public static IndexWriter getIndexWrtier(String directoryPath, IndexWriterConfig config) {
	    FSDirectory directory = openFSDirectory(directoryPath);
	    return luceneService.getIndexWriter(directory, config);
	  }
	  
	  /**
	   * 获取IndexReader
	   * @param dir
	   * @param enableNRTReader  是否开启NRTReader
	   * @return
	   */
	  public static IndexReader getIndexReader(Directory dir,boolean enableNRTReader) {
	    return luceneService.getIndexReader(dir, enableNRTReader);
	  }
	  
	  /**
	   * 获取IndexReader(默认不启用NRTReader)
	   * @param dir
	   * @return
	   */
	  public static IndexReader getIndexReader(Directory dir) {
	    return luceneService.getIndexReader(dir);
	  }
	  
	  /**
	   * 获取IndexSearcher
	   * @param reader    IndexReader对象
	   * @param executor  如果你需要开启多线程查询，请提供ExecutorService对象参数
	   * @return
	   */
	  public static IndexSearcher getIndexSearcher(IndexReader reader,ExecutorService executor) {
	    return luceneService.getIndexSearcher(reader, executor);
	  }
	  
	  /**
	   * 获取IndexSearcher(不支持多线程查询)
	   * @param reader    IndexReader对象
	   * @return
	   */
	  public static IndexSearcher getIndexSearcher(IndexReader reader) {
	    return luceneService.getIndexSearcher(reader);
	  }
	 
	  /**
	   * 创建QueryParser对象
	   * @param field
	   * @param analyzer
	   * @return
	   */
	  public static QueryParser createQueryParser(String field, Analyzer analyzer) {
	    return new QueryParser(Version.LUCENE_47,field, analyzer);
	  }
	  
	  /**
	   * 关闭IndexReader
	   * @param reader
	   */
	  public static void closeIndexReader(IndexReader reader) {
	    if (null != reader) {
	      try {
	        reader.close();
	        reader = null;
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    }
	  }
	  
	  /**
	   * 关闭IndexWriter
	   * @param writer
	   */
	  public static void closeIndexWriter(IndexWriter writer) {
	    if(null != writer) {
	      try {
	        writer.close();
	        writer = null;
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    }
	  }
	  
	  /**
	   * 关闭IndexReader和IndexWriter
	   * @param reader
	   * @param writer
	   */
	  public static void closeAll(IndexReader reader, IndexWriter writer) {
	    closeIndexReader(reader);
	    closeIndexWriter(writer);
	  }
	  
	  /**
	   * 删除索引[注意：请自己关闭IndexWriter对象]
	   * @param writer
	   * @param field
	   * @param value
	   */
	  public static void deleteIndex(IndexWriter writer, String field, String value) {
	    try {
	      writer.deleteDocuments(new Term[] {new Term(field,value)});
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 删除索引[注意：请自己关闭IndexWriter对象]
	   * @param writer
	   * @param query
	   */
	  public static void deleteIndex(IndexWriter writer, Query query) {
	    try {
	      writer.deleteDocuments(query);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 批量删除索引[注意：请自己关闭IndexWriter对象]
	   * @param writer
	   * @param terms
	   */
	  public static void deleteIndexs(IndexWriter writer,Term[] terms) {
	    try {
	      writer.deleteDocuments(terms);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 批量删除索引[注意：请自己关闭IndexWriter对象]
	   * @param writer
	   * @param querys
	   */
	  public static void deleteIndexs(IndexWriter writer,Query[] querys) {
	    try {
	      writer.deleteDocuments(querys);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 删除所有索引文档
	   * @param writer
	   */
	  public static void deleteAllIndex(IndexWriter writer) {
	    try {
	      writer.deleteAll();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 更新索引文档
	   * @param writer
	   * @param term
	   * @param document
	   */
	  public static void updateIndex(IndexWriter writer,Term term,Document document) {
	    try {
	      writer.updateDocument(term, document);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  /**
	   * 更新索引文档
	   * @param writer
	   * @param term
	   * @param document
	   */
	  public static void updateIndex(IndexWriter writer,String field,String value,Document document) {
	    updateIndex(writer, new Term(field, value), document);
	  }
	  
	  /*public  boolean buildIndexer(Analyzer analyzer,Directory directory,List<T> items)
		{
			IndexWriter iwriter=null;
			try
			{
				//配置索引
				IndexWriterConfig config=new IndexWriterConfig(Version.LUCENE_47,analyzer);
				config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);//设置打开索引模式为创建或追加
				iwriter=new IndexWriter(directory,config);
				//删除所有document
				iwriter.deleteAll();
				
				Document doc[]=new Document[items.size()];
				for(int i=0;i<items.size();i++)
				{
					doc[i]=new Document();
					T item=items.get(i);
					java.lang.reflect.Field[] fields=item.getClass().getDeclaredFields();
					for(java.lang.reflect.Field field:fields)
					{
						String fieldName=field.getName();
						String getMethodName="get"+toFirstLetterUpperCase(fieldName);
						Object obj=item.getClass().getMethod(getMethodName).invoke(item);
						FieldType fieldtype=new FieldType();
						fieldtype.setIndexed(true);//是否索引
						fieldtype.setStored(true);//是否存储
						fieldtype.setTokenized(true);//是否分类
						doc[i].add(new Field(fieldName,(String)obj,fieldtype));
						
					}
					iwriter.updateDocument(null, doc[i]);
					//iwriter.addDocument(doc[i]);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
			finally{
				try
				{
					iwriter.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			return true;
		}
	  
	  public  List<T> searchIndexer(Analyzer analyzer,Directory directory,String keyword,Class<T> c)
		{
			DirectoryReader ireader=null;
			List<T> result=new ArrayList<T>();
			try {
				//设定搜索目录
				ireader=DirectoryReader.open(directory);
				IndexSearcher isearcher=new IndexSearcher(ireader);
				//对多field进行搜索
				java.lang.reflect.Field[] fields=c.getDeclaredFields();
				int length=fields.length;
				String[]multiFields=new String[length];
				for(int i=0;i<length;i++)
				{
					multiFields[i]=fields[i].getName();
				}
				MultiFieldQueryParser parser=new MultiFieldQueryParser(Version.LUCENE_47,multiFields,analyzer);
				//设定具体的搜索词
				Query query=parser.parse(keyword);
				ScoreDoc[] hits=isearcher.search(query, null,10).scoreDocs;
				for (int i = 0; i < hits.length; i++) {
					Document hitDoc = isearcher.doc(hits[i].doc);
					T item=c.newInstance() ;
					for (String field : multiFields) {
						String setMethodName = "set"+toFirstLetterUpperCase(field);
						item.getClass().getMethod(setMethodName, String.class).invoke(item, hitDoc.get(field));
					}
					result.add(item);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			finally
			{
				try
				{
					ireader.close();
					directory.close();
				}
				catch(Exception e)
				{
					
				}
			}
			return result;
		}*/
	  /**
	   * 创建索引
	   * @param items
	   * @return
	   */
	  public boolean buildIndexer(List<T> items)
	  {
		  IndexWriter iwriter=getIndexWrtier(writePath,getIndexWriterConfig());
		  try
			{
				Document doc[]=new Document[items.size()];
				for(int i=0;i<items.size();i++)
				{
					doc[i]=new Document();
					T item=items.get(i);
					java.lang.reflect.Field[] fields=item.getClass().getDeclaredFields();
					for(java.lang.reflect.Field field:fields)
					{
						String fieldName=field.getName();
						String getMethodName="get"+toFirstLetterUpperCase(fieldName);
						Object obj=item.getClass().getMethod(getMethodName).invoke(item);
						FieldType fieldtype=new FieldType();
						fieldtype.setIndexed(true);//是否索引
						fieldtype.setStored(true);//是否存储
						fieldtype.setTokenized(true);//是否分类
						doc[i].add(new Field(fieldName,(String)obj,fieldtype));
						
					}
					iwriter.addDocument(doc[i]);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
			finally{
				try
				{
					iwriter.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			return true;
	  }
	  /**
	   * 创建，更新索引
	   * @param iwriter
	   * @param items
	   * @return
	   */
	  public boolean buildIndexer(IndexWriter iwriter,List<T> items)
		{
			try
			{
				Document doc[]=new Document[items.size()];
				for(int i=0;i<items.size();i++)
				{
					doc[i]=new Document();
					T item=items.get(i);
					java.lang.reflect.Field[] fields=item.getClass().getDeclaredFields();
					for(java.lang.reflect.Field field:fields)
					{
						String fieldName=field.getName();
						String getMethodName="get"+toFirstLetterUpperCase(fieldName);
						Object obj=item.getClass().getMethod(getMethodName).invoke(item);
						FieldType fieldtype=new FieldType();
						fieldtype.setIndexed(true);//是否索引
						fieldtype.setStored(true);//是否存储
						fieldtype.setTokenized(true);//是否分类
						doc[i].add(new Field(fieldName,(String)obj,fieldtype));
						
					}
					//iwriter.updateDocument(null, doc[i]);
					iwriter.addDocument(doc[i]);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
			finally{
				try
				{
					//iwriter.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			return true;
		}
	  
	  public  List<T> searchIndexer(IndexSearcher isearcher,String keyword,Class<T> c,boolean isHigh)
		{
			List<T> result=new ArrayList<T>();
			try {
				//设定搜索目录
				//对多field进行搜索
//				java.lang.reflect.Field[] fields=c.getDeclaredFields();
//				int length=fields.length;
//				String[]multiFields=new String[length];
//				for(int i=0;i<length;i++)
//				{
//					multiFields[i]=fields[i].getName();
//				}
				//MultiFieldQueryParser parser=new MultiFieldQueryParser(Version.LUCENE_47,multiFields,analyzer);
				//设定具体的搜索词
				Query query=getMultiQuery(keyword,c);//parser.parse(keyword);
				ScoreDoc[] hits=isearcher.search(query, null,10).scoreDocs;
				for (int i = 0; i < hits.length; i++) {
					Document hitDoc = isearcher.doc(hits[i].doc);
					T item=c.newInstance() ;
					for (String field : multiFields) {
						String setMethodName = "set"+toFirstLetterUpperCase(field);
						if(isHigh)
						{
							item.getClass().getMethod(setMethodName, String.class).invoke(item, toHighlighter(query,hitDoc,field));
						}
						else
						{
							item.getClass().getMethod(setMethodName, String.class).invoke(item, hitDoc.get(field));
						}
					}
					result.add(item);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
			return result;
		}
	  /**
	   * 添加索引文档
	   * @param writer
	   * @param doc
	   */
	  public static void addIndex(IndexWriter writer, Document document) {
	    updateIndex(writer, null, document);
	  }
	  /**
	   * 
	   * @param query
	   * @return
	   */
	  public List<Document> query(Query query)
	  {
		  TopDocs topDocs = null;
		  IndexReader reader=getMultiReader(readPaths);
		  IndexSearcher searcher=getIndexSearcher(reader);
		    try {
		      topDocs = searcher.search(query, Integer.MAX_VALUE);
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		    ScoreDoc[] scores = topDocs.scoreDocs;
		    int length = scores.length;
		    if (length <= 0) {
		      return Collections.emptyList();
		    }
		    List<Document> docList = new ArrayList<Document>();
		    try {
		      for (int i = 0; i < length; i++) {
		        Document doc = searcher.doc(scores[i].doc);
		        docList.add(doc);
		      }
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		    return docList;
	  }
	  /**
	   * 索引文档查询
	   * @param searcher
	   * @param query
	   * @return
	   */
	  public List<Document> query(IndexSearcher searcher,Query query) {
	    TopDocs topDocs = null;
	    try {
	      topDocs = searcher.search(query, Integer.MAX_VALUE);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    ScoreDoc[] scores = topDocs.scoreDocs;
	    int length = scores.length;
	    if (length <= 0) {
	      return Collections.emptyList();
	    }
	    List<Document> docList = new ArrayList<Document>();
	    try {
	      for (int i = 0; i < length; i++) {
	        Document doc = searcher.doc(scores[i].doc);
	        docList.add(doc);
	      }
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return docList;
	  }
	  
	  /**
	   * 返回索引文档的总数[注意：请自己手动关闭IndexReader]
	   * @param reader
	   * @return
	   */
	  public static int getIndexTotalCount(IndexReader reader) {
	    return reader.numDocs();
	  }
	  
	  /**
	   * 返回索引文档中最大文档ID[注意：请自己手动关闭IndexReader]
	   * @param reader
	   * @return
	   */
	  public static int getMaxDocId(IndexReader reader) {
	    return reader.maxDoc();
	  }
	  
	  /**
	   * 返回已经删除尚未提交的文档总数[注意：请自己手动关闭IndexReader]
	   * @param reader
	   * @return
	   */
	  public static int getDeletedDocNum(IndexReader reader) {
	    return getMaxDocId(reader) - getIndexTotalCount(reader);
	  }
	  
	  /**
	   * 根据docId查询索引文档
	   * @param reader         IndexReader对象
	   * @param docID          documentId
	   * @param fieldsToLoad   需要返回的field
	   * @return
	   */
	  public static Document findDocumentByDocId(IndexReader reader,int docID, Set<String> fieldsToLoad) {
	    try {
	      return reader.document(docID, fieldsToLoad);
	    } catch (IOException e) {
	      return null;
	    }
	  }
	  
	  /**
	   * 根据docId查询索引文档
	   * @param reader         IndexReader对象
	   * @param docID          documentId
	   * @return
	   */
	  public static Document findDocumentByDocId(IndexReader reader,int docID) {
	    return findDocumentByDocId(reader, docID, null);
	  }
	  
	  /**
	   * @Title: createHighlighter
	   * @Description: 创建高亮器
	   * @param query             索引查询对象
	   * @param prefix            高亮前缀字符串
	   * @param stuffix           高亮后缀字符串
	   * @param fragmenterLength  摘要最大长度
	   * @return
	   */
	  public static Highlighter createHighlighter(Query query, String prefix, String stuffix, int fragmenterLength) {
	    Formatter formatter = new SimpleHTMLFormatter((prefix == null || prefix.trim().length() == 0) ? 
	      "<font color=\"red\">" : prefix, (stuffix == null || stuffix.trim().length() == 0)?"</font>" : stuffix);
	    Scorer fragmentScorer = new QueryScorer(query);
	    Highlighter highlighter = new Highlighter(formatter, fragmentScorer);
	    Fragmenter fragmenter = new SimpleFragmenter(fragmenterLength <= 0 ? 50 : fragmenterLength);
	    highlighter.setTextFragmenter(fragmenter);
	    return highlighter;
	  }
	  
	  /**
	   * @Title: highlight
	   * @Description: 生成高亮文本
	   * @param document          索引文档对象
	   * @param highlighter       高亮器
	   * @param analyzer          索引分词器
	   * @param field             高亮字段
	   * @return
	   * @throws IOException
	   * @throws InvalidTokenOffsetsException
	   */
	  public static String highlight(Document document,Highlighter highlighter,Analyzer analyzer,String field) throws IOException {
	    List<IndexableField> list = document.getFields();
	    for (IndexableField fieldable : list) {
	      String fieldValue = fieldable.stringValue();
	      if(fieldable.name().equals(field)) {
	        try {
	          fieldValue = highlighter.getBestFragment(analyzer, field, fieldValue);
	        } catch (InvalidTokenOffsetsException e) {
	          fieldValue = fieldable.stringValue();
	        }
	        return (fieldValue == null || fieldValue.trim().length() == 0)? fieldable.stringValue() : fieldValue;
	      }
	    }
	    return null;
	  }
	  
	  protected static String toHighlighter(Query query, Document doc, String field) {
			try {
			SimpleHTMLFormatter simpleHtmlFormatter = new SimpleHTMLFormatter("<font color=\"blue\">", "</font>");
			Highlighter highlighter = new Highlighter(simpleHtmlFormatter, new QueryScorer(query));  
	        TokenStream tokenStream1 = analyzer.tokenStream("text", new StringReader(doc.get(field)));  
	        String highlighterStr = highlighter.getBestFragment(tokenStream1, doc.get(field));  
	        return highlighterStr == null ? doc.get(field) : highlighterStr;  
			} catch (IOException e) {  
	            e.printStackTrace();  
	        } catch (InvalidTokenOffsetsException e) {  
	            e.printStackTrace();  
	        }  
	        return null;
		}
	  /**
	   * @Title: searchTotalRecord
	   * @Description: 获取符合条件的总记录数
	   * @param query
	   * @return
	   * @throws IOException
	   */
	  public static int searchTotalRecord(IndexSearcher search,Query query) {
	    ScoreDoc[] docs = null;
	    try {
	      TopDocs topDocs = search.search(query, Integer.MAX_VALUE);
	      if(topDocs == null || topDocs.scoreDocs == null || topDocs.scoreDocs.length == 0) {
	        return 0;
	      }
	      docs = topDocs.scoreDocs;
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return docs.length;
	  }
	  /**
	   * 
	   * @param searcher
	   * @param query
	   * @param page
	   * @return
	   */
	  public ScoreDoc getAfterDoc(IndexSearcher searcher,Query query,Page<Document> page)
	  {
		  ScoreDoc scoreDoc=null; 
		  try {
			TopDocs result=searcher.search(query, Integer.MAX_VALUE);
			//上一页的最后一个document索引
			  int index=(page.getCurrentPage()-1)*page.getPageSize();  
			  //如果当前页是第一页面scoreDoc=null。  
			  if(index>0){  
			      //因为索引是从0开始所以要index-1  
			      scoreDoc=result.scoreDocs[index-1];  
			  }
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		  return scoreDoc;
	  }
	  /**
	   * 分页查询
	   * @param query
	   * @param page
	   */
	  public void pageQuery(Query query,Page<Document> page) {
		IndexSearcher searcher=getIndexSearcher(getMultiReader(readPaths));
	    int totalRecord = searchTotalRecord(searcher,query);
	    //设置总记录数
	    page.setTotalRecord(totalRecord);
	    TopDocs topDocs = null;
	    try {
	    	
	      page.setAfterDoc(getAfterDoc(searcher,query,page));
	      topDocs = searcher.searchAfter(page.getAfterDoc(),query, page.getPageSize());
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    List<Document> docList = new ArrayList<Document>();
	    ScoreDoc[] docs = topDocs.scoreDocs;
	    int index = 0;
	    for (ScoreDoc scoreDoc : docs) {
	      int docID = scoreDoc.doc;
	      
	      Document document = null;
	      try {
	        document = searcher.doc(docID);
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	      if(index == docs.length - 1) {
	        page.setAfterDoc(scoreDoc);
	        page.setAfterDocId(docID);
	      }
	      docList.add(document);
	      index++;
	    }
	    page.setItems(docList);
	    closeIndexReader(searcher.getIndexReader());
	  }
	  /**
	   * 高亮显示翻页
	   * @param query
	   * @param page
	   * @param highlighterParam
	   * @return
	   * @throws IOException
	   */
	  public void pageQuery(Query query,Page<Document> page,HighlighterParam highlighterParam) throws IOException {
		IndexSearcher searcher=getIndexSearcher(getMultiReader(readPaths));
	    List<Document> list=new ArrayList<Document>();
	    //若未设置高亮
	    if(null == highlighterParam || !highlighterParam.isHighlight()) {
	      pageQuery(searcher,query, page);
	    } else {
	      int totalRecord = searchTotalRecord(searcher,query);
	      System.out.println("totalRecord:" + totalRecord);
	      //设置总记录数
	      page.setTotalRecord(totalRecord);
	      page.setAfterDoc(getAfterDoc(searcher,query,page));
	      TopDocs topDocs = searcher.searchAfter(page.getAfterDoc(),query, page.getPageSize());
	      List<Document> docList = new ArrayList<Document>();
	      ScoreDoc[] docs = topDocs.scoreDocs;
	      int index = 0;
	      for (ScoreDoc scoreDoc : docs) {
	        int docID = scoreDoc.doc;
	        Document document = searcher.doc(docID);
	        String strs[]=highlighterParam.getFieldName();
	        for(String str:strs)
	        {
		        String content = document.get(str);
		        if(null != content && content.trim().length() > 0) {
		          //创建高亮器
		          Highlighter highlighter = LuceneUtils.createHighlighter(query,highlighterParam.getPrefix(), highlighterParam.getStuffix(),highlighterParam.getFragmenterLength());
		          String text = highlight(document, highlighter, analyzer, str);
		          document.removeField(str);
		          document.add(new TextField(str,text,Field.Store.YES));
		        }
	        }
	        if(index == docs.length - 1) {
	          page.setAfterDoc(scoreDoc);
	          page.setAfterDocId(docID);
	        }
	        docList.add(document);
	        index++;
	      }
	      page.setItems(docList);
	    }
	    closeIndexReader(searcher.getIndexReader());
	  }
	  /**
	   * @Title: pageQuery
	   * @Description: Lucene分页查询
	   * @param searcher
	   * @param query
	   * @param page
	   * @throws IOException
	   */
	  public void pageQuery(IndexSearcher searcher,Query query,Page<Document> page) {
	    int totalRecord = searchTotalRecord(searcher,query);
	    //设置总记录数
	    page.setTotalRecord(totalRecord);
	    TopDocs topDocs = null;
	    try 
	    {
	      page.setAfterDoc(getAfterDoc(searcher,query,page));
	      topDocs = searcher.searchAfter(page.getAfterDoc(),query, page.getPageSize());
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    List<Document> docList = new ArrayList<Document>();
	    ScoreDoc[] docs = topDocs.scoreDocs;
	    int index = 0;
	    for (ScoreDoc scoreDoc : docs) {
	      int docID = scoreDoc.doc;
	      Document document = null;
	      try {
	        document = searcher.doc(docID);
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	      if(index == docs.length - 1) {
	        page.setAfterDoc(scoreDoc);
	        page.setAfterDocId(docID);
	      }
	      docList.add(document);
	      index++;
	    }
	    page.setItems(docList);
	    closeIndexReader(searcher.getIndexReader());
	  }
	  
	  public void pageQuery(IndexSearcher searcher,Query query,Page<Document> page,HighlighterParam highlighterParam) throws IOException {
		    //IndexWriter writer = null;
		    List<Document> list=new ArrayList<Document>();
		    //若未设置高亮
		    if(null == highlighterParam || !highlighterParam.isHighlight()) {
		      pageQuery(searcher,query, page);
		    } else {
		      int totalRecord = searchTotalRecord(searcher,query);
		      System.out.println("totalRecord:" + totalRecord);
		      //设置总记录数
		      page.setTotalRecord(totalRecord);
		      page.setAfterDoc(getAfterDoc(searcher,query,page));
		      TopDocs topDocs = searcher.searchAfter(page.getAfterDoc(),query, page.getPageSize());
		      List<Document> docList = new ArrayList<Document>();
		      ScoreDoc[] docs = topDocs.scoreDocs;
		      int index = 0;
		      for (ScoreDoc scoreDoc : docs) {
		        int docID = scoreDoc.doc;
		        Document document = searcher.doc(docID);
		        String strs[]=highlighterParam.getFieldName();
		        for(String str:strs)
		        {
			        String content = document.get(str);
			        if(null != content && content.trim().length() > 0) {
			          //创建高亮器
			          Highlighter highlighter = LuceneUtils.createHighlighter(query,highlighterParam.getPrefix(), highlighterParam.getStuffix(),highlighterParam.getFragmenterLength());
			          String text = highlight(document, highlighter, analyzer, str);
			          document.removeField(str);
			          document.add(new TextField(str,text,Field.Store.YES));
			        }
		        }
		        if(index == docs.length - 1) {
		          page.setAfterDoc(scoreDoc);
		          page.setAfterDocId(docID);
		        }
		        docList.add(document);
		        index++;
		      }
		      page.setItems(docList);
		    }
		    closeIndexReader(searcher.getIndexReader());
		  }
	 
	  /**
	   * @Title: pageQuery
	   * @Description: 分页查询[如果设置了高亮,则会更新索引文档]
	   * @param searcher
	   * @param directory
	   * @param query
	   * @param page
	   * @param highlighterParam
	   * @param writerConfig
	   * @throws IOException
	   */
	  /*public static void pageQuery(IndexSearcher searcher,Directory directory,Query query,Page<Document> page,HighlighterParam highlighterParam,IndexWriterConfig writerConfig) throws IOException {
	    IndexWriter writer = null;
	    //若未设置高亮
	    if(null == highlighterParam || !highlighterParam.isHighlight()) {
	      pageQuery(searcher,directory,query, page);
	    } else {
	      int totalRecord = searchTotalRecord(searcher,query);
	      System.out.println("totalRecord:" + totalRecord);
	      //设置总记录数
	      page.setTotalRecord(totalRecord);
	      TopDocs topDocs = searcher.searchAfter(page.getAfterDoc(),query, page.getPageSize());
	      List<Document> docList = new ArrayList<Document>();
	      ScoreDoc[] docs = topDocs.scoreDocs;
	      int index = 0;
	      writer = getIndexWrtier(directory, writerConfig);
	      for (ScoreDoc scoreDoc : docs) {
	        int docID = scoreDoc.doc;
	        Document document = searcher.doc(docID);
	        String content = document.get(highlighterParam.getFieldName());
	        if(null != content && content.trim().length() > 0) {
	          //创建高亮器
	          Highlighter highlighter = LuceneUtils.createHighlighter(query,highlighterParam.getPrefix(), highlighterParam.getStuffix(),highlighterParam.getFragmenterLength());
	          String text = highlight(document, highlighter, analyzer, highlighterParam.getFieldName());
	          //若高亮后跟原始文本不相同，表示高亮成功
	          if(!text.equals(content)) {
	        	  System.out.println(text+"========111111======="+content);
	            Document tempdocument = new Document();
	            List<IndexableField> indexableFieldList = document.getFields();
	            if(null != indexableFieldList && indexableFieldList.size() > 0) {
	              for(IndexableField field : indexableFieldList) {
	            	  System.out.println(field.name()+"========2222222======="+highlighterParam.getFieldName());
	                if(field.name().equals(highlighterParam.getFieldName())) {
	                  tempdocument.add(new TextField(field.name(), text, Field.Store.YES));
	                } else {
	                  tempdocument.add(field);
	                }
	              }
	            }
	            //updateIndex(writer, new Term(highlighterParam.getFieldName(),content), tempdocument);
	            document = tempdocument;
	          }
	        }
	        if(index == docs.length - 1) {
	          page.setAfterDoc(scoreDoc);
	          page.setAfterDocId(docID);
	        }
	        docList.add(document);
	        index++;
	      }
	      page.setItems(docList);
	    }
	    closeIndexReader(searcher.getIndexReader());
	    closeIndexWriter(writer);
	  }*/
	  
	  
	/**
	 * 首字母转大写
	 * @param str
	 * @return
	 */
	public static String toFirstLetterUpperCase(String str)
	{
		if(str == null || str.length() < 2){
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
	}
	public String[] getMultiFields() {
		return multiFields;
	}
	public void setMultiFields(String[] multiFields) {
		this.multiFields = multiFields;
	}

	public String getWritePath() {
		return writePath;
	}

	public void setWritePath(String writePath) {
		this.writePath = writePath;
	}

	public String[] getReadPatharr() {
		return readPatharr;
	}

	public void setReadPath(String[] readPatharr) {
		this.readPatharr = readPatharr;
	}

	public String getReadPaths() {
		return readPaths;
	}

	public void setReadPaths(String readPaths) {
		this.readPaths = readPaths;
	}

	
}
