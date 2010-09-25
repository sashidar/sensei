package com.sensei.indexing.api;

import java.lang.reflect.Method;
import java.text.Format;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

import proj.zoie.api.indexing.ZoieIndexable;

import com.sensei.indexing.api.DefaultSenseiInterpreter.IndexSpec;
import com.sensei.indexing.api.DefaultSenseiInterpreter.MetaFormatSpec;

public class DefaultSenseiZoieIndexable<V> implements ZoieIndexable {

	private static final Logger logger = Logger.getLogger(DefaultSenseiZoieIndexable.class);
	
	private final V _obj;
	private final DefaultSenseiInterpreter<V> _interpreter;
	private final Class<V> _cls;
	
	DefaultSenseiZoieIndexable(V obj,Class<V> cls,DefaultSenseiInterpreter<V> interpreter){
		_obj = obj;
		_interpreter = interpreter;
		_cls = cls;
	}
	
	@Override
	public IndexingReq[] buildIndexingReqs() {
		Document doc = new Document();
		Set<Entry<String,IndexSpec>> entries =  _interpreter._textIndexingSpecMap.entrySet();
		for (Entry<String,IndexSpec> entry : entries){
		  try{
		    String name = entry.getKey();
		    IndexSpec idxSpec = entry.getValue();
		    String val = String.valueOf(idxSpec.fld.get(_obj));
		    doc.add(new org.apache.lucene.document.Field(name,val,idxSpec.store,idxSpec.index,idxSpec.tv));
		  }
		  catch(Exception e){
			logger.error(e.getMessage(),e);
		  }
		}
		Set<Entry<String,MetaFormatSpec>> metaEntries = _interpreter._metaFormatSpecMap.entrySet();
		for (Entry<String,MetaFormatSpec> entry : metaEntries){
		  try{
		    String name = entry.getKey();
		    MetaFormatSpec formatSpec = entry.getValue();
		    
		    Format formatter = formatSpec.formatter;
		    
		    Object valObj = formatSpec.fld.get(_obj);
		    String val = formatter==null ? String.valueOf(valObj) : formatter.format(valObj);
		    
		    org.apache.lucene.document.Field fld = new org.apache.lucene.document.Field(name,val,Store.NO,Index.NOT_ANALYZED_NO_NORMS,TermVector.NO);
		    fld.setOmitTermFreqAndPositions(true);
		    doc.add(fld);
		  }
		  catch(Exception e){
			logger.error(e.getMessage(),e);
		  }
		}
		return new IndexingReq[]{new IndexingReq(doc)};
	}

	@Override
	public long getUID() {
		try {
			return _interpreter._uidField.getLong(_obj);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(),e);
		}
	}
	
	private boolean checkViaReflection(Method m){
		boolean retVal = false;
		if (m!=null){
			try {
				Object retObj = m.invoke(_obj, null);
				retVal = ((Boolean)retObj).booleanValue();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(),e);
			}
		}
		return retVal;
	}

	@Override
	public boolean isDeleted() {
		return checkViaReflection(_interpreter._deleteChecker);
	}

	@Override
	public boolean isSkip() {

		return checkViaReflection(_interpreter._skipChecker);
	}

}
