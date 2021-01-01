#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  bool hasPathSum(TreeNode *root, int sum) {
    if (!root) return false;
    
    if (root->val == sum && !root->left && !root->right) {
      return true;
    }
    return hasPathSum(root->left, sum-root->val) ||
      hasPathSum(root->right, sum-root->val);
  }
};

TreeNode *tree()
{
  TreeNode *n0 = new TreeNode(5);
  TreeNode *n1 = new TreeNode(4);
  TreeNode *n2 = new TreeNode(8);
  TreeNode *n3 = new TreeNode(11);
  TreeNode *n4 = new TreeNode(13);
  TreeNode *n5 = new TreeNode(4);
  TreeNode *n6 = new TreeNode(7);
  TreeNode *n7 = new TreeNode(2);
  TreeNode *n8 = new TreeNode(1);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n2->left = n4;
  n2->right = n5;

  n3->left = n6;
  n3->right = n7;
  n5->right = n8;

  return n0;
}

TreeNode *tree1()
{
  TreeNode *n0 = new TreeNode(1);
  TreeNode *n1 = new TreeNode(2);

  n0->left = n1;
  return n0;
}

int main()
{
  Solution s;
  bool b;
  TreeNode *root;

  root = tree();

  b = s.hasPathSum(root, 22);
  std::cout << "{5,4,8,11,#,13,4,7,2,#,1} has path sum 22 " << (b ? "TRUE" : "FALSE") << "\n";

  b = s.hasPathSum(root, 19);
  std::cout << "{5,4,8,11,#,13,4,7,2,#,1} has not path sum 19 " << (!b ? "TRUE" : "FALSE") << "\n";

  b = s.hasPathSum(NULL, 0);
  std::cout << "{} has not path sum 0 " << (!b ? "TRUE" : "FALSE") << "\n";
  
  b = s.hasPathSum(tree1(), 1);
  std::cout << "{1,2,#} has not path sum 1 " << (!b ? "TRUE" : "FALSE") << "\n";

  return 0;
}  
