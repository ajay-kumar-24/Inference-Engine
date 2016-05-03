import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
 class inference {

	private static BufferedReader br;
	static int counter=0;
	public static List<Predicate> separate(String predicate,boolean right)
	{
		String r1="[()]";
		Pattern p1 = Pattern.compile(r1);
		String replace="---";
		String l1="[^]";
		String inp;
		List<Predicate> pre= new ArrayList<Predicate>();
		Matcher m = p1.matcher(predicate);
		inp=m.replaceAll(replace);
		List<String> args=null;
		String [] arg=null;
		List<String> left=null;
		boolean not=false;
		List<String> pred_list = new ArrayList<String>();
		String pred="";
		if(right==true)
		{
			
			pred_list.add(inp);
		}
		else
		{	
			String [] left_and= inp.split("\\^");
			for(int i =0;i<left_and.length;i++)
			{
			left_and[i]=left_and[i].trim();
			}
			left= new ArrayList<String>(Arrays.asList(left_and));
			for(String a:left)
			{
				if(a.equals("^")==false)
				{
					pred_list.add(a);
				}
			}
		}
		for(String st:pred_list)
		{
			
			String[] brac_spt= st.split(replace); // -----
			List<String> brac= new ArrayList<String>(Arrays.asList(brac_spt));
			if (brac.get(0).charAt(0)=='~')
			{
				not=true;
				pred="~"+Character.toString(brac.get(0).charAt(1));
			}
			else
			{
				not=false;
				pred=brac.get(0);
			}
				if(brac.size()>1)
				{
				arg= brac.get(1).split(",");
				}
				args= new ArrayList<String>(Arrays.asList(arg));
				pre.add(new Predicate(pred,args,not) );
		}
			return pre ;
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		String file= args[1];
		List<Sentence> KB= new ArrayList<Sentence>();
		InputStream filein=new FileInputStream(file); 
        InputStreamReader ipsr=new InputStreamReader(filein);
        br = new BufferedReader(ipsr);
        List<Predicate> queries = new ArrayList<Predicate>();
        int n= Integer.parseInt(br.readLine());
        for(int i=0;i<n;i++)
        {
        queries.addAll(separate(br.readLine(), true));	
        }
        n= Integer.parseInt(br.readLine());
        Map<String,String> sent_list= new HashMap<String,String>();
        
        for(int i=0 ;i <n ;i++)
        {
        	String s = br.readLine();
        	String[] results= s.split("=>");
        	List<Predicate> left= new ArrayList<Predicate>();
            List<Predicate> right= new ArrayList<Predicate>();
        	if(results.length>1)
        	{
        		results[1]=results[1].trim();
        		results[0]=results[0].trim();
        		right.addAll(separate(results[1].trim(),true));
        		left.addAll(separate(results[0].trim(),false));
        	}
        	else
        	{
        		s=s.trim();
        		right.addAll(separate(s.trim(),true));
        		left=null;
        	}
        	Sentence kb = new Sentence(left,right);
        	KB.add(i,standardize(kb,inference.counter++));
        	}
        HashMap<String,List<Integer>> tree=new HashMap<String,List<Integer>>();
        HashMap<String,Integer> visited=new HashMap<String,Integer>();
        for(Sentence i :KB)
        {
        	
        	if(tree.containsKey(i.right.get(0).name))
        	{
        	List<Integer> index = new ArrayList<Integer>();
        	if(tree.get(i.right.get(0).name)==null)
        	{
        		index.add(new Integer(KB.indexOf(i)));
        	}
        	else
        	{
        		index= tree.get(i.right.get(0).name);
        		index.add(new Integer(KB.indexOf(i)));
        	}
        	}
        	else
        	{
        		List<Integer> no = new ArrayList<Integer>();
        		no.add(KB.indexOf(i));
        		tree.put(i.right.get(0).name, no);
        	}
        	
        }
        KnowledgeBase kb = new KnowledgeBase(KB,tree,visited);
        PrintWriter writer = new PrintWriter(args[2], "UTF-8");
        for(int i=0;i<queries.size();i++)
		{
        	List<HashMap<String,String>> res= new ArrayList<HashMap<String,String>>();
			res=do_BC_ASK(kb,queries.get(i));
			if(res.size()==0)
			{
				writer.println("FALSE");
			}
			else
			{
				writer.println("TRUE");
			}
		}
        writer.close();
	}
	public static Sentence standardize(Sentence sentence,int counter)
	{
		String count =String.valueOf(counter);
		Predicate right=sentence.right.get(0);
		for(int i=0;i<right.args.size();i++)
		{
			if(right.args.get(i).matches("^[a-z].*$")){
			String arguement=right.args.get(i);
			Matcher match = Pattern.compile("[^0-9]*([0-9]+).*").matcher(arguement);
			if (match.matches()) {
				right.args.remove(i);
				right.args.add(i,arguement.substring(0,arguement.indexOf(match.group(1)))+count);
			}
			else
			{
				right.args.remove(i);
				right.args.add(i,arguement+count);
			}
			}
		}
		if(sentence.left!=null)
		for(int i=0;i<sentence.left.size();i++)
		{
			Predicate left=sentence.left.get(i);
			for(int j=0;j<left.args.size();j++)
			{
				if(left.args.get(j).matches("^[a-z].*$")){
				String arguement=left.args.get(j);
				Matcher match = Pattern.compile("[^0-9]*([0-9]+).*").matcher(arguement);
				if (match.matches()) {
					left.args.remove(j);
					left.args.add(j,arguement.substring(0,arguement.indexOf(match.group(1)))+count);
				}
				else
				{
					left.args.remove(j);
					left.args.add(j,arguement+count);
				}
				}
			}
		}
		return sentence;
	}
	public static boolean isFact(Sentence s)
	{
		if(s.left==null)
			return true;
		else
			return false;
	}
	public static Predicate sub(Predicate goal,HashMap<String,String> teta)
	{
		List<String> arg= new ArrayList<String>();
		boolean not=false;
		for(int i=0;i<goal.args.size();i++)
		{
			if(goal.args.get(i).matches("^[a-z].*$"))
			{
				String c_val=goal.args.get(i);
				while(teta.containsKey(c_val))
					c_val=teta.get(c_val);
				arg.add(c_val);
			}
			else
			{
				arg.add(goal.args.get(i));
			}
			
			if(goal.name.charAt(0)=='~')
				not =true;
			
		}
		Predicate pred =new Predicate(goal.name,arg,not);
		return pred;
	}
	public static String toString(Predicate s)
	{
		String st = "";
		st+=s.name+"*";
		for(int i=0;i<s.args.size();i++)
		{
			if(s.args.get(i).matches("^[A-Z].*$"))
				st+=s.args.get(i)+"*";
			else 
				return null;
		}
		return st;
	}
	
	public static HashMap<String,String> unify(List<String> list1,List<String> list2,HashMap<String,String> teta)
	{
		if(teta==null)
			return null;
		if(list1.size()!=list2.size())
			return null;
		if(list1.size()==1)
		{
			if(list1.get(0).matches("^[A-Z].*$") && list2.get(0).matches("^[A-Z].*$"))
			{
				if(list1.get(0).equals(list2.get(0)))
					return teta;
				else
					return null;
			}
			if(list1.get(0).matches("^[a-z].*$"))
				return unification_var(list1,teta,list2);
			if(list2.get(0).matches("^[a-z].*$"))
				return unification_var(list2,teta,list1);
		}
		else{
			List<String> t1=new ArrayList<String>();
			t1.add(list1.get(0));
			List<String> t2=new ArrayList<String>();
			t2.add(list2.get(0));
			list1.remove(0);
			list2.remove(0);
			 return unify(list1,list2,unify(t1,t2,teta));
		}
		return null;
		
	}
	public static HashMap<String,String> unification_var(List<String> variable,HashMap<String,String> teta,List<String> arg)
	{
		List<String> val=new ArrayList<String>();
		if(teta.containsKey(variable.get(0)))
		{
			val.add(teta.get(variable.get(0)));
			return unify(val,arg,teta);
		}
		else if(teta.containsKey(arg.get(0)))
		{
			val.add(teta.get(arg.get(0)));
			return unify(variable,val,teta);
		}

			teta.put(variable.get(0),arg.get(0));
			return teta;
		}
	public static List<HashMap<String,String>> do_BC_ASK(KnowledgeBase kb,Predicate goal)
	{
		List<HashMap<String,String>> res= new ArrayList<HashMap<String,String>>();
		res=  do_BC_OR(kb,goal,new HashMap<String,String>());
		return res;
	}
	public static List<HashMap<String,String>> do_BC_OR(KnowledgeBase kb,Predicate goal,HashMap<String,String> teta)
	{
		List<Integer> indexes=kb.tree.get(goal.name);
		List<HashMap<String,String>> list=new ArrayList<HashMap<String,String>>();
		if(indexes!=null)
		for(int i=0;i<indexes.size();i++)
		{
			HashMap<String,String> temp=new HashMap<String,String>(teta);
			Sentence sentence=standardize(kb.s.get(indexes.get(i)),inference.counter++);
			List<String> list1=new ArrayList<String>(goal.args);
			List<String> list2=new ArrayList<String>(sentence.right.get(0).args);
			temp=unify(list1,list2,temp);
			if(temp!=null)
			{   
				if(sentence.left!=null)
				{
					List<HashMap<String,String>> templist=new ArrayList<HashMap<String,String>>();
					templist=do_BC_AND(kb,sentence.left,temp);
					if(templist!=null)
					{
						list.addAll(templist);
					}
				}
				else
				{
					list.add(temp);
				}
			}
		}
		
			return list;
	}
	public static List<HashMap<String,String>> do_BC_AND(KnowledgeBase kb,List<Predicate> goal,HashMap<String,String> teta)
	{
		List<HashMap<String,String>> list=new ArrayList<HashMap<String,String>>();
		if(teta==null)
			{
			return null;
			}
		else if(goal.size()==0)
		{
			list.add(teta);
			return list;
		}
		else
		{
			List<Predicate> l1=new ArrayList<Predicate>();
			l1.add(goal.get(0));
			List<Predicate> others=new ArrayList<Predicate>(goal);
			others.remove(0);
			Predicate new_goal=sub(l1.get(0),teta);
			String visited_st=toString(new_goal);
			if(visited_st!=null)
			{
			if(kb.visited.containsKey(visited_st) && kb.visited.get(visited_st).intValue()==1)
			{
			     return null;
			}
			kb.visited.put(visited_st,new Integer(1));
			}
			//System.out.println(new_goal.name+" new goal  "+teta);
	     	List<HashMap<String,String>> new_list=do_BC_OR(kb,sub(new_goal,teta),teta);
			if(visited_st!=null)
				kb.visited.put(visited_st,new Integer(0));
			for(HashMap<String,String> teta_n:new_list)
			{
				List<HashMap<String,String>> l2=do_BC_AND(kb,others,teta_n);
				if(l2!=null)
					list.addAll(l2);
					
			}
		}
		return list;
	}	
}
class Sentence{
	List<Predicate> left,right;
	Sentence(List<Predicate> left,List<Predicate> right)
	{
		this.left=left;
		this.right=right;
	}
	public Sentence() {
		// TODO Auto-generated constructor stub
	}
}
class Predicate{
	List<String> args;
	String name;
	Boolean isNot;
	Predicate(String name,List<String> args,Boolean isNot)
	{
		this.name=name;
		this.args=args;
		this.isNot=isNot;
	}
}
class KnowledgeBase {
	List<Sentence> s;
	HashMap<String,List<Integer>> tree;
	HashMap<String,Integer> visited;
	KnowledgeBase(List<Sentence> s,HashMap<String,List<Integer>> tree,HashMap<String,Integer> visited)
	{
		this.s=s;
		this.tree=tree;
		this.visited=visited;
	}

}

