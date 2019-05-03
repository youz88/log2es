package com.youz.log.service;

import com.youz.log.annotation.Document;
import com.youz.log.util.U;
import com.youz.log.util.page.Page;
import com.youz.log.util.page.PageInfo;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Component
public class ElasticSearchService {

//	@Autowired
//	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	private String id;

	public static TransportClient client = null;

	@Value("${log.elasticsearch.host:localhost}")
	private String host;

	@Value("${log.elasticsearch.port:9300}")
	private Integer port;

	private static final SimpleDateFormat SDF_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	public Client getClient() {
		try {
			if(client == null) {
				client = new PreBuiltTransportClient(Settings.EMPTY)
				        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host),port));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return client;
	}

	public static void close() {
		client.close();
	}

	public HighlightBuilder getHighlight(String... parms) {
		HighlightBuilder hBuilder = null;
		if(parms != null || parms.length != 0) {
			hBuilder = new HighlightBuilder();
			for(String parm:parms) {
				hBuilder.field(parm);

			}
	        hBuilder.preTags("<span style=\"color:red\">");
	        hBuilder.postTags("</span>");
		}

        return hBuilder;
	}

	/**
	 * 添加索引
	 * @param object	实体
	 */
	public void addIndex(Object object) {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		Document document = getDocument(clazz);
//		threadPoolTaskExecutor.execute(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
//					for(Field field:fields) {
//						String fieldName = field.getName();
//						Method method = clazz.getDeclaredMethod("get"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1));
//						builder.field(fieldName, method.invoke(object));
//					}
//					builder.endObject();
//					String index = document.index();
//					if(index.contains("$")){
//						String dateFormat = index.substring(index.indexOf("${")+2,index.indexOf("}"));
//						index = index.substring(0,index.indexOf("$")) + DateUtil.formatDate(new Date(),dateFormat);
//					}
//					getClient().prepareIndex(index, document.type(), id).setSource(builder).get();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
	}

	/**
	 * 搜索
	 * @param search    关键词
	 * @param page    分页参数
  	 * @param clazz    返回数据类型
	 * @param showHigh    是否高亮
	 * @param searchField    搜索字段
	 * @return
	 */
	public PageInfo getPageInfoBySearch(String search, Page page, Class<?> clazz, Boolean showHigh, String... searchField){
		Document document = getDocument(clazz);
		List list = new ArrayList();
		long total = 0;
		try {
			MultiMatchQueryBuilder queryBuilder = null;
			if (U.isNotBlank(search)) {
				queryBuilder = QueryBuilders.multiMatchQuery(search, searchField);
			}
			String index = document.index();
			if(index.contains("$")){
				index = index.substring(0,index.indexOf("$")) + "*";
			}
			SearchResponse searchResponse = getClient()
					.prepareSearch(index)
					.setTypes(document.type())
					.setQuery(queryBuilder)
					.setFrom((page.getPage()-1) * page.getLimit())
					.setSize(page.getLimit())
					.highlighter(showHigh ? getHighlight(searchField) : null)
					.execute()
					.actionGet();
			total = searchResponse.getHits().getTotalHits();
			//设置高亮字段
			for (SearchHit hit : searchResponse.getHits()) {
				Object newInstance = assemblyClazz(hit,clazz,showHigh);
				list.add(newInstance);
				newInstance = null;
			}
		}finally {
//			ElasticSearchUtil.close();
		}
		return new PageInfo(total,list);
	}

	/**
	 *
	 * @param clazz
	 * @return
	 */
	public Object getDetails(Class<?> clazz) {
		U.assertNil(id,"查询的ID不能为空");
		Object newInstance = null;
		Document document = getDocument(clazz);
		String index = document.index();
		if(index.contains("$")){
			index = index.substring(0,index.indexOf("$")) + "*";
		}
		SearchResponse response = getClient()
				.prepareSearch(index)
				.setTypes(document.type())
				.setQuery(QueryBuilders.matchQuery("id", id))
				.execute()
				.actionGet();
		if(response.getHits().totalHits == 1) {
			SearchHit at = response.getHits().getAt(0);
			newInstance = assemblyClazz(at,clazz,false);
		}
		return newInstance;
	}

	/**
	 * 后台管理查询（根据所传对象的属性值全匹配）
	 * @param objectParam	查询的对象
	 * @param page	分页参数
	 * @param clazz	返回数据类型
	 * @return
	 */
	public PageInfo getPageInfoByManage(Object objectParam, Integer page, Integer limit, Class<?> clazz) {
		Document document = getDocument(clazz);
		List list = new ArrayList();
		long total = 0;
		String index = document.index();
		if (index.contains("$")) {
			index = index.substring(0, index.indexOf("$")) + "*";
		}
		SearchResponse searchResponse = getClient()
				.prepareSearch(index)
				.setTypes(document.type())
				.setQuery(assemblyQueryBuilder(objectParam))
				.setFrom((page - 1) * limit)
				.setSize(limit)
				.addSort(SortBuilders.fieldSort("@timestamp"))//根据创建时间排序
				.execute()
				.actionGet();
		total = searchResponse.getHits().getTotalHits();
		for (SearchHit hit : searchResponse.getHits()) {
			list.add(assemblyClazz(hit,clazz,false));
		}
		return new PageInfo(total,list);
	}

	private Document getDocument(Class<?> clazz){
		Document document = clazz.getAnnotation(Document.class);
		U.assertNil(document,clazz+"异常,未找到@Document标识");
		return document;
	}

	private QueryBuilder assemblyQueryBuilder(Object objectParam){
		Class<?> paramClass = objectParam.getClass();
		Field[] declaredFields = paramClass.getDeclaredFields();
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		for(Field field:declaredFields){
			try {
				Method declaredMethod = paramClass.getDeclaredMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
				Object invoke = declaredMethod.invoke(objectParam);
				if(U.isNotBlank(invoke)){
					queryBuilder.must(QueryBuilders.termQuery(field.getName(),invoke.toString()));
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return queryBuilder;
	}

	private Object assemblyClazz(SearchHit hit, Class<?> clazz, Boolean showHigh)  {
		Object newInstance = null;
		try {
			newInstance = clazz.newInstance();
			Map<String, Object> source = hit.getSource();
			for(Entry<String, Object> entry:source.entrySet()) {
				if(entry.getKey().startsWith("@")) continue;//Kibana自动创建了[@version、@timestamp]
				Class<?> fieldType = clazz.getDeclaredField(entry.getKey()).getType();
				Method method = clazz.getDeclaredMethod(
						"set" + entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1), fieldType);
				if(showHigh){
					Map<String, HighlightField> result = hit.getHighlightFields();
					HighlightField highField = result.get(entry.getKey());
					if (highField !=null) {
						String html = "";
						// 取得定义的高亮标签
						Text[] titleTexts = highField.fragments();
						// 为title串值增加自定义的高亮标签
						for (Text text : titleTexts) {
							html += text;
						}
						method.invoke(newInstance, html);
					}else {
						if(fieldType.getSimpleName().equalsIgnoreCase("Date")) {
							method.invoke(newInstance, ElasticSearchService.SDF_UTC.parse(entry.getValue()+""));
						}else {
							method.invoke(newInstance, entry.getValue());
						}
					}
				}else{
					if (fieldType.getSimpleName().equalsIgnoreCase("Date")) {
						method.invoke(newInstance, ElasticSearchService.SDF_UTC.parse(entry.getValue() + ""));
					} else {
						method.invoke(newInstance, entry.getValue());
					}
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return newInstance;
	}

}
