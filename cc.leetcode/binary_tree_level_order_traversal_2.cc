#include <iostream>
#include <vector>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;

  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  void levelN(std::vector< std::vector<int> > &vv, TreeNode *root, size_t i) {
    if (!root) return;
    
    if (i >= vv.size()) vv.resize(i+1);
    vv[i].push_back(root->val);

    levelN(vv, root->left, i+1);
    levelN(vv, root->right, i+1);
  }
  
  std::vector< std::vector<int> > levelOrderBottom(TreeNode *root) {
    std::vector< std::vector<int> > vv;
    if (!root) return vv;

    levelN(vv, root, 0);

    for (size_t start = 0, end = vv.size(); start+1 < end; start++, end--) {
      vv[start].swap(vv[end-1]);
    }
    return vv;
  }
};

TreeNode *tree()
{
  TreeNode *n0 = new TreeNode(3);
  TreeNode *n1 = new TreeNode(9);
  TreeNode *n2 = new TreeNode(20);
  TreeNode *n3 = new TreeNode(15);
  TreeNode *n4 = new TreeNode(7);

  n0->left = n1;
  n0->right = n2;

  n2->left = n3;
  n2->right = n4;

  return n0;
}

TreeNode *tree2()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(2);
  TreeNode *n2 = new TreeNode(4);
  TreeNode *n3 = new TreeNode(1);
  TreeNode *n4 = new TreeNode(3);
  TreeNode *n5 = new TreeNode(-1);
  TreeNode *n6 = new TreeNode(5);
  TreeNode *n7 = new TreeNode(1);
  TreeNode *n8 = new TreeNode(6);
  TreeNode *n9 = new TreeNode(8);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n2->left = n4;
  n2->right = n5;

  n3->left = n6;
  n3->right = n7;
  n4->right = n8;
  n5->right = n9;

  return n0;
}

void printMatrix(const std::vector< std::vector<int> > &matrix)
{
  for (size_t i = 0; i < matrix.size(); ++i) {
    std::cout << "[";
    for (size_t j = 0; j < matrix[i].size(); ++j) {
      std::cout << matrix[i][j];
      if (j + 1 < matrix[i].size()) std::cout << ",";
    }
    std::cout << "]";
  }
}

int main()
{
  Solution s;
  
  TreeNode *root = tree();
  std::vector< std::vector<int> > vv = s.levelOrderBottom(root);

  if (vv[0][0] == 15 && vv[0][1] == 7 &&
      vv[1][0] == 9 && vv[1][1] == 20 &&
      vv[2][0] == 3) {
    std::cout << "levelOrderBottom {3,9,20,#,#,15,7} TRUE\n";
  } else {
    std::cout << "levelOrderBottom {3,9,20,#,#,15,7} FALSE\n";
  }

  printMatrix(s.levelOrderBottom(tree2()));

  return 0;  
}
  
