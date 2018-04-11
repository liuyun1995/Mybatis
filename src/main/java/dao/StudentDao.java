package dao;

import entity.Student;

public interface StudentDao {

	Student selectById(String sId);
	
	int update(Student student);
	
	int delete(Student student);
	
	int addStudent(Student student);
	
}
