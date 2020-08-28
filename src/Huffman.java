import java.io.*;
import java.util.*;

public class Huffman {
    public static void main(String[] args) throws Exception {

        if(args.length < 2)
        {
            throw new Exception("Improper number of args");
        }

        boolean compress = Boolean.parseBoolean(args[0]);
        String filename = args[1];
        if(compress) {
            encode(new File(filename));
        }
        else {
            decodeTree(new File(filename));
        }
    }

    private static void encode(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            Map<Character, Integer> counts = new HashMap<>();

            int i;
            while((i = br.read()) != -1) {
                char c = (char)i;
                if(counts.containsKey(c)) {
                    counts.put(c,counts.get(c) + 1);
                }
                else {
                    counts.put(c,1);
                }
            }
            br.close();

            Node[] sorted = new Node[counts.size()];
            Set<Map.Entry<Character,Integer>> entries = counts.entrySet();
            i=0;
            for(Map.Entry<Character,Integer> entry: entries) {
                int j = i - 1;
                while(j >= 0 && sorted[j].freq < entry.getValue()) {
                    sorted[j + 1] = sorted[j];
                    j--;
                }
                sorted[j + 1] = new Node(entry.getKey(),entry.getValue());
                i++;
            }

            for(int j = sorted.length - 1; j > 1; j--)
            {
                Node one = sorted[j];
                Node two = sorted[j-1];

                Node combined = new Node(one.freq + two.freq);
                combined.left = one;
                combined.right = two;

                sorted[j] = null;
                sorted[j - 1] = null;

                int k = j-2;
                while (k >= 0 && sorted[k].freq < combined.freq)
                {
                    sorted[k + 1] = sorted[k];
                    k--;
                }
                sorted[k + 1] = combined;
            }

            Node root = new Node(sorted[0].freq + sorted[1].freq);
            root.left = sorted[0];
            root.right = sorted[1];
            sorted[0] = root;
            sorted[1] = null;


            Map<Character,String> bits = new HashMap<>();
            findBits(root,bits,"");

            Queue<Byte> encoding = new LinkedList<>();

            String upNext = "";
            br = new BufferedReader(new FileReader(f));
            int a;
            while((a = br.read()) != -1)
            {
                char c = (char)a;
                upNext += bits.get(c);
                while(upNext.length() >= 8)
                {
                    String byt = upNext.substring(0,8);
                    upNext = upNext.substring(8);
                    encoding.add(parseByte(byt));
                }
            }

            while (upNext.length() != 0 && upNext.length() < 8)
            {
                upNext += "0";
            }
            if(upNext.length() != 0) {
                encoding.add(parseByte(upNext));
            }

            br.close();

            byte[] b = new byte[encoding.size()];
            for(int j = 0; j < b.length;j++)
            {
                b[j] = encoding.remove();
            }

            String tree = preorder(root);
           // System.out.println(tree.replace('\u0000','*'));
            FileOutputStream stream = new FileOutputStream("compressed.bin",false);
            byte[] bytes = tree.getBytes();
            int count = bytes.length;
            stream.write(count);
            stream.write(tree.getBytes());
            stream.write(b);
            stream.close();
        } catch (IOException fnfe) {
            fnfe.printStackTrace();
        }
    }

    private static String preorder(Node root)
    {
        String ret = "" + root.c;

        if(root.c == '\u0000')
        {
            ret += preorder(root.left);
            ret += preorder(root.right);
        }

        return ret;
    }

    private static byte parseByte(String s)
    {

        char[] nouveau = s.toCharArray();
        boolean negative = nouveau[0] == '1';

        if(!negative)
        {
            return Byte.parseByte(s,2);
        }

        for(int i = 0; i < 8;i++)
        {
            nouveau[i] = nouveau[i] == '1'?'0':'1';
        }

        for(int i = 7; i >= 0;i--)
        {
            if(i == 0)
            {
                return -128;
            }
            if(nouveau[i] == '1')
            {
                nouveau[i] = '0';
            }
            else {
                nouveau[i] = '1';
                break;
            }
        }

        StringBuilder res = new StringBuilder();
        for(char c:nouveau)
        {
            res.append(c);
        }
        return (byte)(-1 * Byte.parseByte(res.toString(),2));
    }

    private static void findBits(Node root, Map<Character,String> bits, String soFar)
    {
        if(root.isLeaf)
        {
            bits.put(root.c,soFar);
            return;
        }

        findBits(root.left,bits,soFar + "0");
        findBits(root.right,bits,soFar + "1");
    }

    private static void basicPre(TreeNode node)
    {
        System.out.print(node.c + " ");
        if(!node.isLeaf)
        {
            basicPre(node.left);
            basicPre(node.right);
        }
    }

    private static void decodeTree(File f) {
        try {
            BufferedInputStream bs = new BufferedInputStream(new FileInputStream(f));

            int treeSize = bs.read();
            List<Character> treePreOrder = new ArrayList<>();

            System.out.println("1) Getting Tree");
            for(int i = 0; i < treeSize;i++)
            {
                char c = (char)bs.read();
                treePreOrder.add(c);
            }

            TreeNode root = constructTree(treePreOrder);

            System.out.println("2) Decoding Text");

            StringBuilder originalBuilder = new StringBuilder();

            int a;
            int index = 0;
            while((a = bs.read()) != -1)
            {
                for(int i = 7;i >= 0; i--)
                {
                    //true = right, false = left;
                    boolean dir = (a & (1 << i)) == (1 << i);

                    index *= 2;
                    index += dir?2:1;

                    TreeNode node = root.get(index);
                    //System.out.println(index);
                    char ch = node.c;
                    //System.out.println((ch == '\u0000'?"*":ch) + " " + (dir?1:0));
                    if(ch != '\u0000') {
                        originalBuilder.append(ch);
                        index = 0;
                    }
                }
            }

            String original = originalBuilder.toString();

            PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter("output.txt")));
            output.print(original);
            output.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static TreeNode constructTree(List<Character> preorder)
    {
        TreeNode root = new TreeNode(preorder.get(0));
        Stack<TreeNode> inners = new Stack<>();
        inners.push(root);

        String s = "";
        for(Character c:preorder)
        {
            s+=c;
        }
        //System.out.println(s.replace('\u0000','*'));

        int i = 1;
        while(!inners.isEmpty())
        {
            //System.out.println(inners);
            char c = preorder.get(i++);
            TreeNode node = new TreeNode(c);
            TreeNode parent = inners.peek();
            if(parent.left != null)
            {
                parent.right = node;
                inners.pop();
            }
            else {
                parent.left = node;
            }

            if(!node.isLeaf)
            {
                inners.push(node);
            }
        }
        return root;
    }

    private static class TreeNode {
        char c;
        boolean isLeaf;
        TreeNode left;
        TreeNode right;

        public TreeNode(char c)
        {
            this.c = c;
            this.isLeaf = c != '\u0000';
        }

        public TreeNode get(int index)
        {
            if(index == 0)
            {
                return this;
            }

            Stack<Integer> stack = new Stack<>();
            int temp = index;
            stack.push(temp);
            while(temp > 0)
            {
                temp = (temp - 1)/2;
                stack.push(temp);
            }

            TreeNode current = this;
            int previous = stack.pop();
            while(!stack.isEmpty())
            {
                temp = stack.pop();
                if(previous * 2 + 1 == temp) {
                    current = current.left;
                }
                else {
                    current = current.right;
                }
                previous = temp;
            }

            return current;
        }
    }

    private static class Node {
        int freq;
        char c;
        boolean isLeaf;
        Node left;
        Node right;

        public Node(char c, int freq)
        {
            this.c = c;
            this.freq = freq;
            isLeaf = true;
        }

        public Node(int freq)
        {
            isLeaf = false;
            this.freq = freq;
        }

        public Node()
        {

        }

        public String toString()
        {
            return isLeaf?c + ":" + freq:freq + "--";
        }
    }
}
