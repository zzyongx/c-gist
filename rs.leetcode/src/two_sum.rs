pub struct Solution();

impl Solution {
    pub fn two_sum(nums: Vec<i32>, target: i32) -> Vec<i32> {
        for i in 0..nums.len() {
            for j in i + 1..nums.len() {
                if nums[i] + nums[j] == target {
                    return vec![i as i32, j as i32];
                }
            }
        }
        return Vec::new();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn two_sum() {
        let nums = vec![2, 7, 11, 15];
        let v = Solution::two_sum(nums.clone(), 9);
        assert_eq!(v.len(), 2);
        assert_eq!(v[0], 0);
        assert_eq!(v[1], 1);

        let v = Solution::two_sum(nums.clone(), 22);
        assert_eq!(v[0], 1);
        assert_eq!(v[1], 3);
    }
}
