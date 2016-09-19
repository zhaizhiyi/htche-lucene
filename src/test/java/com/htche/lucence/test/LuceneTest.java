package com.htche.lucence.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.junit.Test;

import com.htche.lucence.util.HighlighterParam;
import com.htche.lucence.util.LuceneUtils;
import com.htche.lucence.util.Page;

public class LuceneTest {

	LuceneUtils lu=new LuceneUtils();
	String[] luceneDir=new String[]{"d:/lucene","d:/lucene2"};
	String writerDir="d:/lucene";
	IndexReader indexReader=lu.getMultiReader(luceneDir);
	public void writeIndex()
	{
		try 
		{
			
			IndexWriter iwriter=lu.getIndexWrtier(writerDir, lu.getIndexWriterConfig());
			List<Product> items=new ArrayList<Product>();
			items.add(new Product("8","毛衣","冬天的"));
			items.add(new Product("9","毛线","过冬了"));
			lu.buildIndexer(iwriter, items);
			iwriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void buildIndex()
	{
		List<Product> items=new ArrayList<Product>();
		for(int i=0;i<56;i++)
		{
			items.add(new Product(i+"","毛衣毛库"+i,"冬天到了"+i));
		}
		lu.buildIndexer(items);
	}
	@Test
	public void pageQuery()
	{
		try {
			Page<Document> page=new Page<Document>();
			HighlighterParam hp=new HighlighterParam();
			String[] fieldarr=new String[]{"descr","name"};
			hp.setFieldName(fieldarr);
			hp.setHighlight(true);
			Query query=lu.getMultiQuery("毛",Product.class);
			page.setCurrentPage(2);
			lu.pageQuery(query,page,hp);
			/*for(int i=1;i<page.getTotalPage();i++)
			{
				page.setCurrentPage(i);
				lu.pageQuery(query,page);
			}*/
			Collection<Document> items=page.getItems();
			for(Document doc:items)
			{
				System.out.println("--------------"+doc.get("id")+","+doc.get("name")+"======"+doc.get("descr"));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void readIndex()
	{
		Query query=lu.getMultiQuery("毛",Product.class);
		IndexSearcher isearcher=new IndexSearcher(indexReader);
		List<Product> listpd=lu.searchIndexer(isearcher,null,Product.class,true);
		for(Product pd:listpd)
		{
			System.out.println(pd.getDescr());
		}
	}
	
	public void queryIndex()
	{
		Query query=lu.getMultiQuery("毛",Product.class);
		List<Document> list=lu.query(query);
		for(Document doc:list)
		{
			System.out.println("id = "+doc.get("id")+",name = "+doc.get("name")+",descr = "+doc.get("descr"));
		}
	}
	/**
	 * 多索引目录，翻页查询
	 */
	public void pageReadIndex()
	{
		try {
			Page<Document> page=new Page<Document>();
			HighlighterParam hp=new HighlighterParam();
			Query query=lu.getMultiQuery("毛",Product.class);
			IndexSearcher isearcher=new IndexSearcher(indexReader);
			String[] fieldarr=new String[]{"descr","name"};
			hp.setFieldName(fieldarr);
			hp.setHighlight(true);
			//hp.setPrefix("<font color=\"blue\">");
			//hp.setStuffix("</font>");
			lu.pageQuery(isearcher,query,page,hp);
			Collection<Document> items=page.getItems();
			for(Document doc:items)
			{
				System.out.println("--------------"+doc.get("id")+","+doc.get("name")+"======"+doc.get("descr"));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 多索引目录，多线程查询,异步收集查询结果
	 */
	public void multiThreadreadIndex()
	{
		
		int count=2;
		ExecutorService pool = Executors.newFixedThreadPool(count);
		IndexReader indexReader=lu.getMultiReader(luceneDir);
		final IndexSearcher isearcher=LuceneUtils.getIndexSearcher(indexReader, pool);
		final Query query=lu.getMultiQuery("毛",Product.class);
		final Page<Document> page=new Page<Document>();
		
		List<Future<Collection<Document>>> futures=new ArrayList<Future<Collection<Document>>>(count);
		 for(int i=0;i<count;i++)
		 {
			 futures.add(pool.submit(new Callable<Collection<Document>>(){
				 public Collection<Document> call() throws Exception
				 {
					List<Document> list= lu.query(isearcher,query);
					page.setItems(list);
					return list;
				 }
			 }));
		 }
		 int t=0;
		 try 
		 {
			for(Future<Collection<Document>> future:futures)
			 {
				 Collection<Document> list=future.get();
				 if(null==list||list.size()<=0)
				 {
					 t++;
					 continue;
				 }
				 for(Document doc:list)
				 {
					 System.out.println(doc.get("descr"));
				 }
				 System.out.println("");
			 }
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		 if(t==count)
		 {
			 System.out.println("No results.");
		 }
		pool.shutdown();
	}
	
	/**
	 * 查询所有
	 */
	public void matchAllDocsQuery()
	{
		//查询所有,对应的查询字符串为:*:*
		Query query=new MatchAllDocsQuery();
		searchAndShowResult(query);
	}
	/**
	 * 关键词查询
	 */
	public void termQuery()
	{
		//对应的查询字符串为: descr:我
		Query query=new TermQuery(new Term("descr","我"));
		searchAndShowResult(query);
	}
	/**
	 * 通配符查询
	 */
	public void wildcardQuery()
	{
		//通配符查询,?:表示一个任意字符，*:表示0或多个任意字符,对应的查询字符串为:descr:我?
		//对应的查询为:name:fr*nk?
		Query query=new WildcardQuery(new Term("name","fr*nk?"));
		//query=new WildcardQuery(new Term("descr","我*"));//
		searchAndShowResult(query);
	}
	/**
	 * 模糊查询
	 */
	public void fuzzyQuery()
	{
		//模糊查询,对应的查询字符串为：descr:我好~8
		Query query=new FuzzyQuery(new Term("descr","我"),1,1);
		searchAndShowResult(query);
	}
	/**
	 * 布尔查询
	 */
	public void booleanQuery()
	{
		Query query2=new WildcardQuery(new Term("name","fr*nk?"));
		Query query3=new FuzzyQuery(new Term("descr","我"),1,1);
		Query query5=NumericRangeQuery.newIntRange("id", 1, 3, true, true);
		BooleanQuery query6=new BooleanQuery();
		
		query6.add(query2,Occur.MUST);//必须满足
		query6.add(query5, Occur.MUST_NOT);//非
		query6.add(query3,Occur.SHOULD);//多个SHOULD一起用表示OR的关系
		searchAndShowResult(query6);
	}
	/**
	 * 范围查询
	 */
	public void numericRangeQuery()
	{
		//范围查询,对应的查询字符串为:id:[1 TO 3]
		Query query=NumericRangeQuery.newIntRange("id", 1, 3, true, true);
		searchAndShowResult(query);
	}
	private void searchAndShowResult(Query query) {
		try {
			System.out.println("--->  // 对应的查询字符串为：" + query + "\n");
			lu.setWritePath(writerDir);
			DirectoryReader ireader=DirectoryReader.open(lu.getDirectory());
			// 2，执行查询，得到中间结果
			IndexSearcher indexSearcher = new IndexSearcher(ireader); // 指定所用的索引库
			TopDocs topDocs = indexSearcher.search(query, 100); // 最多返回前n条结果
			// 3，处理结果
			List<Product> list = new ArrayList<Product>();
			
			for (int i = 0; i < topDocs.scoreDocs.length; i++) {
				// 根据编号拿到Document数据
				int docId = topDocs.scoreDocs[i].doc; // Document的内部编号
				Document doc = indexSearcher.doc(docId);
				Product pd=new Product(doc.get("id"),doc.get("name"),doc.get("descr"));
				list.add(pd);
			}
			
			System.out.println("总结果数：" + list.size());
			for(Product pd:list)
			{
				System.out.println("id ="+pd.getId());
				System.out.println("name ="+pd.getName());
				System.out.println("descr ="+pd.getDescr());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void phraseQuery()
	{
		try {
//			File file=new File("d:/lucene");
//			Directory directory=FSDirectory.open(file);
//			DirectoryReader ireader=DirectoryReader.open(directory);
			//indexReader=LuceneUtils.getMultiReader(luceneDir);
			/*java.lang.reflect.Field[] fields=Product.class.getDeclaredFields();
			String[]multiFields=new String[fields.length];
			for(int i=0;i<fields.length;i++)
			{
				multiFields[i]=fields[i].getName();
			}*/
			
			
			
			//范围搜索
//			Term beginid=new Term("id","1");
//			Term endid=new Term("id","4");
//			RangeQuery query7=new RangeQuery(beginid,endid,false);
			//使用前缀检索
			Term pre=new Term("name","f");
			//query=new PrefixQuery(pre);
			
			//多关键字搜索
			PhraseQuery query7=new PhraseQuery();
			query7.add(new Term("descr","this"));
			query7.add(new Term("descr","is"));
			query7.setSlop(1);
			//SimpleHTMLFormatter simpleHtmlFormatter = new SimpleHTMLFormatter("<font color=\"blue\">", "</font>");
			//Highlighter highlighter = new Highlighter(simpleHtmlFormatter, new QueryScorer(query));  
			//pageQuery(isearcher,pool,5,query,page,config);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
	}
	public static void main(String[] args)
	{
		LuceneTest test=new LuceneTest();
		//test.lu.setReadPath(test.luceneDir);
		test.queryIndex();
	}
}
