#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};


class Solution {
public:
  int max(int a, int b) {
    return a > b ? a : b;
  }

  int maxDepth(TreeNode *root) {
    if (!root) return 0;
    return max(maxDepth(root->left), maxDepth(root->right)) + 1;
  }
};

TreeNode *tree1()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);
  TreeNode *n4 = new TreeNode(4);
  TreeNode *n5 = new TreeNode(5);
  TreeNode *n6 = new TreeNode(6);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n1->right = n4;

  n2->left = n5;

  n3->right = n6;

  // depth 4
  return n0;
}

TreeNode *tree2()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);
  TreeNode *n4 = new TreeNode(4);
  TreeNode *n5 = new TreeNode(5);

  n0->left = n1;
  n1->right = n2;
  n2->right = n3;
  n3->right = n4;
  n4->left = n5;

  // depth 6
  return n0;
}

int main()
{
  Solution s;
  int n;

  TreeNode *root1 = tree1();
  n = s.maxDepth(root1);
  std::cout << "tree 1 depth " << (n == 4 ? "TRUE" : "FALSE") << "\n";

  TreeNode *root2 = tree2();
  n = s.maxDepth(root2);
  std::cout << "tree 2 depth " << (n == 6 ? "TRUE" : "FALSE") << "\n";

  return 0;
}  
