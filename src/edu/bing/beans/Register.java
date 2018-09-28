package edu.bing.beans;

public class Register {

	private String reg_name;
	private int reg_value;
	private int status;
	
	public String getReg_name() {
		return reg_name;
	}
	public void setReg_name(String reg_name) {
		this.reg_name = reg_name;
	}
	public int getReg_value() {
		return reg_value;
	}
	public void setReg_value(int reg_value) {
		this.reg_value = reg_value;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	@Override
	public String toString() {
		return "Register: Register_name = " + reg_name + ", Register_value = " + reg_value + ", Status = " + status;
	}
	
	
}
