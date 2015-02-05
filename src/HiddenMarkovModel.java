//Author: Sean Daly
//Authored: 9/24/2014
//Description: An implementation of a bigram Hidden Markov Model tagger. 

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;


public class HiddenMarkovModel {

	static Hashtable<String, Double> transitions = 
			new Hashtable<String, Double>();
	static Hashtable<String, Double> emissions = 
			new Hashtable<String, Double>();
	static Hashtable<String, Integer> transCount = 
			new Hashtable<String, Integer>();
	static Hashtable<String, Integer> emisCount = 
			new Hashtable<String, Integer>();
	
	static ArrayList<String> tags = new ArrayList<String>(); //used for viterbi


	public static void main (String args[]) throws FileNotFoundException{
		Scanner trainingReader = new Scanner(new File("training.txt"));
		trainModel(trainingReader);
		Scanner testReader = new Scanner(new File("test.txt"));
		String taggedTestSent = testReader.nextLine();
		testReader.close();
		
		Scanner testReader2 = new Scanner(new File("test.txt"));
		String testSent = removeTags(testReader2);
		
		/*Since this is a bigram tagger, I'm adding an extra character to the
		 * beginning of the sentence.*/
		testSent = "."+" "+testSent;
		ArrayList<String> result = viterbi(testSent);
		String taggedSent = resultTag(testSent, result);
		
		/*for comparison purposes, I remove the ./START from the resulting
		 * tagged sentence*/
		taggedSent = taggedSent.substring(taggedSent.indexOf(" ")+1);
		
		double error = computeError(taggedTestSent, taggedSent)*100;

		System.out.println(result.toString());
		System.out.println("Provided test sentence: "+taggedTestSent);
		System.out.println("My tagged sentence:     "+taggedSent);
		System.out.printf("Error rate: %.2f",error);
		System.out.println("%");
		System.out.println("Unknown words are tagged as NN.");
		


	}

	public static void trainModel(Scanner reader){
		String prevWord = ".";
		String prevTag = "START";
		String em;
		em = prevWord + "/" + prevTag;
		emissions.put(em, 1.0);
		emisCount.put(prevTag, 1);
		tags.add(prevTag);

		while(reader.hasNext()){

			String token = reader.next();
			String thisTag = token.substring(token.indexOf('/')+1);
			updateTables(token, prevTag, thisTag);

			prevTag = thisTag;	
		}

		reader.close();
		computeProbs();	
	}
	
	/*My implementation of the Viterbi algorithm.*/
	public static ArrayList<String> viterbi(String sentence){

		double viterbi[][] = new double[sentence.length()][tags.size()];

		Scanner tokenizer = new Scanner(sentence);
		/*prevProbs and currentLoc are used to keep track of the relevant 
		 * probabilities and tags while computing the backpath.*/
		ArrayList<Double> prevProbs = new ArrayList<Double>();
		ArrayList<String> currentLoc = new ArrayList<String>();
		
		double highestProb = 0;
		double transProb;
		double emisProb;
		int col = 1;
		ArrayList<String> backpath = new ArrayList<String>();
		String currentTag = "";

		//Initialize the matrix.
		String start = tokenizer.next();

		for(int x = 0; x < tags.size(); x++){
			String e = start+"/"+tags.get(x);
			if(e.equals("./START")){
				viterbi[x][0] = emissions.get(e);
				prevProbs.add(viterbi[x][0]);
				backpath.add(tags.get(x));
				currentLoc.add(tags.get(x));
			}
			else{
				viterbi[x][0] = 0.0;
			}
		}
		
		//Compute the viterbi probabilities and generate the backpath.
		int currentPos = 0;
		while(tokenizer.hasNext()){

			String currentWord = tokenizer.next();
			int size = prevProbs.size();

			for(int i = 0; i < size; i++){
				
				for(int k = 1; k < tags.size(); k++){
					String tag = tags.get(k);
					String t = currentLoc.get(i) +" "+ tag;
					
					//no transition.
					if(!(transitions.containsKey(t))){ 
						viterbi[k][col] = 0.0;
						continue;
					}
					//no emission for word
					String e = currentWord+"/"+tag;
					if(!(emissions.containsKey(e))){ 
						viterbi[k][col] = 0.0;
						continue;
					}
					//else there is a transition and emission for the word.
					transProb = transitions.get(t);
					emisProb = emissions.get(e);
					double temp = prevProbs.get(i)*transProb*emisProb;
					
					/*if this is the first time we're computing the probability
					 * for this cell.*/
					if((temp > viterbi[k][col]) && (viterbi[k][col] == 0.0)){
						viterbi[k][col] = temp;
						prevProbs.add(viterbi[k][col]);
						currentLoc.add(tag);
					}
					/*for when we've already computed the probability for this
					 * cell, but may need to update if the new probability is
					 * higher.*/
					else if((temp > viterbi[k][col]) && 
								(viterbi[k][col] != 0.0)){
						prevProbs.set(currentPos,viterbi[k][col]);
						currentLoc.set(currentPos, tag);
						currentPos++;
					}
					/* keep track of highest probability and associated tag for 
					 * the backpath*/
					if(viterbi[k][col] > highestProb){
						highestProb = viterbi[k][col];
						currentTag = tag;
					}

				}
				
			}
			col += 1;
			backpath.add(currentTag);
			highestProb = 0.0;
		}
		tokenizer.close();
		return backpath;
	}

	/*Removes the tags from the test data.*/
	public static String removeTags(Scanner input){
		String sentence = "";
		while(input.hasNext()){
			String token = input.next();
			String word = token.substring(0,token.indexOf("/"));
			sentence = sentence +" "+ word; 
		
		}
		sentence = sentence.trim();
		return sentence;
	}

	/*Helper function for trainModel().*/
	public static void updateTables(String token,String prevTag,String thisTag){

		String bigram = prevTag;

		bigram = bigram +" "+ thisTag;

		if(!(transitions.containsKey(bigram))){
			transitions.put(bigram, 1.0);
		}

		else{
			transitions.put(bigram, transitions.get(bigram)+1);
		}

		if(!(transCount.containsKey(prevTag))){
			transCount.put(prevTag, 1);
		}
		else{
			int count = transCount.get(prevTag);
			transCount.put(prevTag, count+1);
		}

		if(!(emissions.containsKey(token))){
			emissions.put(token, 1.0);
		}

		else{
			emissions.put(token,emissions.get(token)+1);
		}

		if(!(emisCount.containsKey(thisTag))){
			emisCount.put(thisTag, 1);
			tags.add(thisTag);
		}

		else{
			int count = emisCount.get(thisTag);
			emisCount.put(thisTag, count+1);
		}
	}
	
	/*Iterates through the transitions and emissions
	 * and computes the probability of each*/
	public static void computeProbs(){

		Iterator<Map.Entry<String, Double>> transIt = 
				transitions.entrySet().iterator();
		while(transIt.hasNext()){
			Map.Entry<String, Double> entry = transIt.next();
			int end = entry.getKey().indexOf(" ");
			String tag = entry.getKey().substring(0, end);
			int count = transCount.get(tag);
			transitions.put(entry.getKey(), entry.getValue()/count);
		}

		Iterator<Map.Entry<String, Double>> emisIt = 
				emissions.entrySet().iterator();
		while(emisIt.hasNext()){
			Map.Entry<String, Double> entry = emisIt.next();
			int begin = entry.getKey().indexOf('/')+1;
			int end = entry.getKey().length();
			String tag = entry.getKey().substring(begin, end);
			int count = emisCount.get(tag);
			emissions.put(entry.getKey(), entry.getValue()/count);

		}
	}
	
	/*Tags the sentence using the the results from viterbi.*/
	public static String resultTag(String testSent, ArrayList<String> tags){
		Scanner scanner = new Scanner(testSent);
		String result = "";
		int index = 0;
		while(scanner.hasNext()){
			result = result + scanner.next()+"/"+tags.get(index)+" ";
			index++;
		}
		scanner.close();
		return result;
	}
	
	public static double computeError(String taggedTestSent, String taggedSent){
		Scanner test = new Scanner(taggedTestSent);
		Scanner tagged = new Scanner(taggedSent);
		double errorCount = 0.0;
		double totalCount = 0.0;
		double error;
		while(test.hasNext() && tagged.hasNext()){
			if(!(test.next().equals(tagged.next()))){
				errorCount += 1;
			}
			totalCount += 1;
		}
		error = errorCount/totalCount;
		test.close();
		tagged.close();
		return error;
	}
}

