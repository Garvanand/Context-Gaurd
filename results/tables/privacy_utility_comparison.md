# Three-Mode Privacy-Utility Evaluation Table

| Mode                           | Accuracy (%) | Macro F1 | STOP Recall (%) | ACT FAR (%) | Mean Latency (ms) | Transmitted Sensitive Regions | Redaction Ratio (%) | Payload Size (KB) | Bandwidth Reduction (%) |
| :----------------------------- | :----------- | :------- | :-------------- | :---------- | :---------------- | :---------------------------- | :------------------ | :---------------- | :---------------------- |
| Mode 1: ON_DEVICE              | 63.33%       | 0.5389   | 70.00%          | 5.00%       | 18.84             | 0                             | 100.0%              | 0.00              | 100.0%                  |
| Mode 2: REDACTED_LOCAL_BACKEND | 65.00%       | 0.5044   | 90.00%          | 10.00%      | 135.35            | 0                             | 100.0%              | 11.53             | 59.3%                   |
| Mode 3: RAW_CLOUD_EVALUATION   | 68.33%       | 0.5455   | 95.00%          | 10.00%      | 375.33            | 117                           | 0.0%                | 28.35             | 0.0%                    |
