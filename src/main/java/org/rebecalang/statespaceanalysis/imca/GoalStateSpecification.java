package org.rebecalang.statespaceanalysis.imca;

public class GoalStateSpecification{
	private String operator;
	private String value; 
			
	public GoalStateSpecification (String operator, String value){
		this.operator = operator;
		this.value = value;
	}
	
	public String getValue (){
		return this.value;
	}
	public String getOperator (){
		return this.operator;
	}

	
}
