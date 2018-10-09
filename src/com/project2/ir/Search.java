package com.project2.ir;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class Search {
	
	public static HashMap<String, LinkedList<Integer>> invertedIndex = new HashMap<String, LinkedList<Integer>>();
	
	static{
		try{
			Directory fsDir = FSDirectory.open(Paths.get("/Users/praveenkumarrajendran/Courses/IR/Projects/inverted-index/index"));
			DirectoryReader reader = DirectoryReader.open(fsDir);
			Fields indexedFields = MultiFields.getFields(reader);
			for (String idxedField: indexedFields){
				if(idxedField.contains("text_")){
//					System.out.println("Getting terms for field::"+idxedField);
					Terms terms = MultiFields.getTerms(reader,idxedField);
					TermsEnum termsEnum = terms.iterator();
					BytesRef bytesRef = null;
					while((bytesRef = termsEnum.next())!=null){
						try{
							String termString = bytesRef.utf8ToString();
							PostingsEnum postingsEnum = termsEnum.postings(null,PostingsEnum.FREQS);
							int docId = -1;
							LinkedList<Integer> postingsList = new LinkedList<Integer>();
							while((docId = postingsEnum.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS){
								postingsList.add(docId);
							}
							Collections.sort(postingsList);
							invertedIndex.put(termString, postingsList);
						}
						catch(Exception e){
							
							System.out.println("Exception occured while trying to get the term and postings");
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch(IOException ioe){
			System.out.println("Exception occured while trying to read directory");
		}
	}
	public static void main(String[] args){
		
		Search search = new Search();
		search.termAtATimeAnd("ateint feme");
		search.termAtATimeOr("ateint feme");
		search.documentAtATimeAnd("ateint feme");
		search.documentAtATimeOr("ateint feme");
			
//		System.out.println("Inverted index size::"+invertedIndex.size());
	}
	
	private void termAtATimeAnd(String searchQuery){
		
		System.out.println("Entering term at a time And");
		String[] searchTerms = searchQuery.split(" ");
		LinkedList<Integer> results = null;
		for (String searchTerm: searchTerms){
			LinkedList<Integer> termPostings = invertedIndex.get(searchTerm);
//			System.out.println("Postings for "+searchTerm+"::"+termPostings);
			if(results == null){
				results = termPostings;
			}
			else{
				LinkedList<Integer> intermediateResults = new LinkedList<Integer>();
				int i=0;
				int j=0;
//				System.out.println("List sizes::"+results.size()+"::"+termPostings.size());
				while(i< results.size() && j < termPostings.size()){
					int resultsDocId = results.get(i);
					int termPostingDocId =  termPostings.get(j);
					if(resultsDocId == termPostingDocId){
						intermediateResults.add(results.get(i));
						i++;
						j++;
					}
					else if(resultsDocId < termPostingDocId){
						i++;
					}
					else if(termPostingDocId < resultsDocId){
						j++;
					}
				}
				results = intermediateResults;
			}
//			System.out.println("Retrieve postings list for::"+searchTerm);
		}
		System.out.println("TaatAnd result::"+results);
	}
	
	private void termAtATimeOr(String searchQuery){
		
		System.out.println("Entering term at a time Or");
		String[] searchTerms = searchQuery.split(" ");
		LinkedList<Integer> results = null;
		for (String searchTerm: searchTerms){
			LinkedList<Integer> termPostings = invertedIndex.get(searchTerm);
//			System.out.println("Postings for "+searchTerm+"::"+termPostings);
			if(results == null){
				results = termPostings;
			}
			else{
				LinkedList<Integer> intermediateResults = new LinkedList<Integer>();
				int i=0;
				int j=0;
//				System.out.println("List sizes::"+results.size()+"::"+termPostings.size());
				while(i< results.size() && j < termPostings.size()){
					int resultsDocId = results.get(i);
					int termPostingDocId =  termPostings.get(j);
					if(resultsDocId == termPostingDocId){
						intermediateResults.add(resultsDocId);
						i++;
						j++;
					}
					else if(resultsDocId < termPostingDocId){
						intermediateResults.add(resultsDocId);
						i++;
					}
					else if(termPostingDocId < resultsDocId){
						intermediateResults.add(termPostingDocId);
						j++;
					}
				}
				while(i< results.size()){
					intermediateResults.add(results.get(i));
					i++;
				}
				while(j < termPostings.size()){
					intermediateResults.add(termPostings.get(j));
					j++;
				}
//				System.out.println("Intermediate results are::"+intermediateResults);
				results = intermediateResults;
			}
//			System.out.println("Retrieve postings list for::"+searchTerm);
		}
		System.out.println("TaaTOr result::"+results);
	}
	
	private void documentAtATimeAnd(String searchQuery){
		
		System.out.println("Entering document at a time And");
		
		LinkedList<Integer> results = new LinkedList<Integer>();
		HashMap<String, LinkedList<Integer>> termVsPostingsList = new HashMap<String, LinkedList<Integer>>();
		HashMap<String, Integer> termVsCurrentIndex = new HashMap<String, Integer>();
		String[] searchTerms = searchQuery.split(" ");
		int numOfSearchTerms = searchTerms.length;
		for (String searchTerm : searchTerms){
			termVsPostingsList.put(searchTerm, invertedIndex.get(searchTerm));
			termVsCurrentIndex.put(searchTerm, 0);
		}
		int minimumDocId;
		boolean hasAnyListEnded = false;
		while(true){
			
			if(hasAnyListEnded){
				break;
			}
			minimumDocId = -99;
			
			//Use priorityQueue to find the minimum documentId
			for (Map.Entry<String, LinkedList<Integer>> entry: termVsPostingsList.entrySet()){
				String searchTerm = entry.getKey();
				LinkedList<Integer> postingsList = entry.getValue();
				int currentIndex = termVsCurrentIndex.get(searchTerm);
				int currentDocId = postingsList.get(currentIndex);
				if(minimumDocId == -99){
					minimumDocId = currentDocId;
				}
				else if(currentDocId < minimumDocId){
					minimumDocId = currentDocId;
				}
			}
			
			int score = 0;
			for (Map.Entry<String, LinkedList<Integer>> entry: termVsPostingsList.entrySet()){
				String searchTerm = entry.getKey();
				LinkedList<Integer> postingsList = entry.getValue();
				int currentIndex = termVsCurrentIndex.get(searchTerm);
				int currentDocId = postingsList.get(currentIndex);
				if(currentDocId == minimumDocId){
					score++;
					if(currentIndex == (postingsList.size()-1)){
						hasAnyListEnded = true;
					}
					else{
						termVsCurrentIndex.put(searchTerm, currentIndex+1);
					}
				}
			}
			if(score == numOfSearchTerms){
//				System.out.println("Doc with id::"+minimumDocId+":: matches the query");
				results.add(minimumDocId);
			}
			else{
//				System.out.println("Doc with id::"+minimumDocId+":: does not match the query");
			}
		}
		System.out.println("DaatAnd result::"+results);
	}
	
	private void documentAtATimeOr(String searchQuery){
		
		System.out.println("Entering document at a time And");
		LinkedList<Integer> results = new LinkedList<Integer>();
		HashMap<String, LinkedList<Integer>> termVsPostingsList = new HashMap<String, LinkedList<Integer>>();
		HashMap<String, Integer> termVsCurrentIndex = new HashMap<String, Integer>();
		String[] searchTerms = searchQuery.split(" ");
		for (String searchTerm : searchTerms){
			termVsPostingsList.put(searchTerm, invertedIndex.get(searchTerm));
			termVsCurrentIndex.put(searchTerm, 0);
		}
		int minimumDocId;
		while(true){
	
			minimumDocId = -99;
			if(termVsPostingsList.size() == 0){
				break;
			}
			
			//Use priorityQueue to find the minimum documentId
			for (Map.Entry<String, LinkedList<Integer>> entry: termVsPostingsList.entrySet()){
				String searchTerm = entry.getKey();
				LinkedList<Integer> postingsList = entry.getValue();
				int currentIndex = termVsCurrentIndex.get(searchTerm);
				int currentDocId = postingsList.get(currentIndex);
				if(minimumDocId == -99){
					minimumDocId = currentDocId;
				}
				else if(currentDocId < minimumDocId){
					minimumDocId = currentDocId;
				}
			}
//			System.out.println("Minimum document id is::"+minimumDocId);
			
			int score = 0;
			List<String> listsThatHasEnded = new ArrayList<String>();
			for (Map.Entry<String, LinkedList<Integer>> entry: termVsPostingsList.entrySet()){
				String searchTerm = entry.getKey();
				LinkedList<Integer> postingsList = entry.getValue();
				int currentIndex = termVsCurrentIndex.get(searchTerm);
				int currentDocId = postingsList.get(currentIndex);
				if(currentDocId == minimumDocId){
					score++;
					if(currentIndex == (postingsList.size()-1)){
						listsThatHasEnded.add(searchTerm);
					}
					else{
						termVsCurrentIndex.put(searchTerm, currentIndex+1);
					}
				}
			}
			if(score > 0){
//				System.out.println("Doc with id::"+minimumDocId+":: matches the query");
				results.add(minimumDocId);
			}
			else{
//				System.out.println("Doc with id::"+minimumDocId+":: does not match the query");
			}
			if(listsThatHasEnded.size()> 0){
				for (String searchTerm: listsThatHasEnded){
					termVsPostingsList.remove(searchTerm);
				}
			}
		}
		System.out.println("DaatOr result::"+results);
	}
	
}
