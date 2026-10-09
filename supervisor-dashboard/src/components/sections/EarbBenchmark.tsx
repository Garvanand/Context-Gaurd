import React, { useState } from 'react';
import {
  Database,
  Filter,
  Search,
  CheckCircle,
  AlertTriangle,
  HelpCircle,
  XCircle,
} from 'lucide-react';
import type { EarbData } from '../../types';

interface EarbBenchmarkProps {
  data: EarbData | null;
  loading: boolean;
}

export const EarbBenchmark: React.FC<EarbBenchmarkProps> = ({ data, loading }) => {
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [interventionFilter, setInterventionFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  if (loading && !data) {
    return (
      <div style={{ padding: '30px', textAlign: 'center', color: '#64748b' }}>
        Loading EARB benchmark dataset...
      </div>
    );
  }

  const pairs = data?.pairs || [];

  const filteredPairs = pairs.filter((p) => {
    if (categoryFilter !== 'ALL' && p.category !== categoryFilter) return false;
    if (interventionFilter !== 'ALL' && p.expected_intervention !== interventionFilter) return false;
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      return (
        p.pair_id.toLowerCase().includes(term) ||
        p.base_artifact_id.toLowerCase().includes(term) ||
        p.context.toLowerCase().includes(term) ||
        p.intended_action.toLowerCase().includes(term)
      );
    }
    return true;
  });

  const getInterventionBadge = (intv: string) => {
    switch (intv.toUpperCase()) {
      case 'STOP':
        return <span className="badge badge-stop"><XCircle size={10} style={{ marginRight: '3px' }} />STOP</span>;
      case 'WARN':
        return <span className="badge badge-warn"><AlertTriangle size={10} style={{ marginRight: '3px' }} />WARN</span>;
      case 'ASK':
        return <span className="badge badge-ask"><HelpCircle size={10} style={{ marginRight: '3px' }} />ASK</span>;
      case 'ACT':
        return <span className="badge badge-act"><CheckCircle size={10} style={{ marginRight: '3px' }} />ACT</span>;
      default:
        return <span className="badge badge-not-eval">{intv}</span>;
    }
  };

  const categories = data?.categories || {
    FINANCIAL: 15,
    DIGITAL_SECURITY: 15,
    PRIVACY_DISCLOSURE: 15,
    COMMUNICATION: 15,
  };

  const intDist = data?.intervention_distribution || {
    STOP: 18,
    WARN: 16,
    ASK: 14,
    ACT: 12,
  };

  const ambDist = data?.ambiguity_distribution || {
    clear: 46,
    ambiguous: 14,
  };

  return (
    <section id="section-earb" style={{ marginBottom: '32px' }}>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '16px'
      }}>
        <div>
          <h2 style={{
            fontSize: '18px',
            fontWeight: 700,
            color: '#f8fafc',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            margin: 0
          }}>
            <Database size={18} color="#38bdf8" />
            4. Everyday Action Risk Benchmark (EARB)
          </h2>
          <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
            60 paired action-artifact evaluations across 20 synthetic base artifacts and 4 sensitive domains.
          </p>
        </div>
        <div style={{
          fontSize: '11px',
          color: '#38bdf8',
          fontFamily: 'monospace',
          backgroundColor: '#0c121e',
          padding: '4px 10px',
          borderRadius: '4px',
          border: '1px solid #1a253a'
        }}>
          DATASET V1.0 // NO SENSITIVE DATA
        </div>
      </div>

      {/* Overview Cards & Distributions */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '12px',
        marginBottom: '16px'
      }}>
        {/* Total stats */}
        <div className="card" style={{ padding: '16px', backgroundColor: '#0c111e' }}>
          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>PAIRS & ARTIFACTS</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginTop: '6px' }}>
            <span style={{ fontSize: '24px', fontWeight: 700, fontFamily: 'monospace', color: '#f8fafc' }}>
              {data?.total_pairs || 60}
            </span>
            <span style={{ fontSize: '12px', color: '#94a3b8' }}>Pairs /</span>
            <span style={{ fontSize: '18px', fontWeight: 600, fontFamily: 'monospace', color: '#38bdf8' }}>
              {data?.base_artifacts || 20}
            </span>
            <span style={{ fontSize: '12px', color: '#94a3b8' }}>Artifacts</span>
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', marginTop: '8px' }}>
            Each base artifact evaluated across 3 distinct candidate actions.
          </div>
        </div>

        {/* 4 Categories */}
        <div className="card" style={{ padding: '16px', backgroundColor: '#0c111e' }}>
          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, marginBottom: '8px' }}>
            4 RISK DOMAINS (15 EACH)
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '11px' }}>
            {Object.entries(categories).map(([cat, count]) => (
              <div key={cat} style={{ display: 'flex', justifyContent: 'space-between', color: '#cbd5e1' }}>
                <span style={{ color: '#94a3b8' }}>{cat.replace('_', ' ')}</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{count} pairs</span>
              </div>
            ))}
          </div>
        </div>

        {/* Intervention Distribution */}
        <div className="card" style={{ padding: '16px', backgroundColor: '#0c111e' }}>
          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, marginBottom: '8px' }}>
            INTERVENTION DISTRIBUTION
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
            <div style={{ padding: '6px 8px', backgroundColor: '#090d16', borderRadius: '4px', border: '1px solid #1a253a' }}>
              <div style={{ fontSize: '10px', color: '#f87171' }}>STOP</div>
              <div style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'monospace', color: '#f87171' }}>{intDist.STOP || 18}</div>
            </div>
            <div style={{ padding: '6px 8px', backgroundColor: '#090d16', borderRadius: '4px', border: '1px solid #1a253a' }}>
              <div style={{ fontSize: '10px', color: '#fbbf24' }}>WARN</div>
              <div style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'monospace', color: '#fbbf24' }}>{intDist.WARN || 16}</div>
            </div>
            <div style={{ padding: '6px 8px', backgroundColor: '#090d16', borderRadius: '4px', border: '1px solid #1a253a' }}>
              <div style={{ fontSize: '10px', color: '#60a5fa' }}>ASK</div>
              <div style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'monospace', color: '#60a5fa' }}>{intDist.ASK || 14}</div>
            </div>
            <div style={{ padding: '6px 8px', backgroundColor: '#090d16', borderRadius: '4px', border: '1px solid #1a253a' }}>
              <div style={{ fontSize: '10px', color: '#34d399' }}>ACT</div>
              <div style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'monospace', color: '#34d399' }}>{intDist.ACT || 12}</div>
            </div>
          </div>
        </div>

        {/* Ambiguity Distribution */}
        <div className="card" style={{ padding: '16px', backgroundColor: '#0c111e' }}>
          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, marginBottom: '8px' }}>
            AMBIGUITY DISTRIBUTION
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '6px' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94a3b8' }}>
                <span>Unambiguous (Clear Risk / Safe)</span>
                <span style={{ fontFamily: 'monospace', color: '#f8fafc' }}>{ambDist.clear || 46}</span>
              </div>
              <div style={{ width: '100%', height: '4px', backgroundColor: '#1e293b', borderRadius: '2px', marginTop: '4px' }}>
                <div style={{ width: `${((ambDist.clear || 46) / 60) * 100}%`, height: '100%', backgroundColor: '#38bdf8' }} />
              </div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94a3b8' }}>
                <span>Ambiguous (Needs Intent Resolution)</span>
                <span style={{ fontFamily: 'monospace', color: '#f8fafc' }}>{ambDist.ambiguous || 14}</span>
              </div>
              <div style={{ width: '100%', height: '4px', backgroundColor: '#1e293b', borderRadius: '2px', marginTop: '4px' }}>
                <div style={{ width: `${((ambDist.ambiguous || 14) / 60) * 100}%`, height: '100%', backgroundColor: '#f59e0b' }} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#0c111e',
        padding: '10px 14px',
        borderRadius: '6px',
        border: '1px solid #1a253a',
        marginBottom: '12px',
        flexWrap: 'wrap',
        gap: '10px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#94a3b8' }}>
            <Filter size={13} />
            <span>Category:</span>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              style={{ fontSize: '11px', padding: '4px 8px' }}
            >
              <option value="ALL">ALL CATEGORIES</option>
              <option value="FINANCIAL">FINANCIAL</option>
              <option value="DIGITAL_SECURITY">DIGITAL SECURITY</option>
              <option value="PRIVACY_DISCLOSURE">PRIVACY DISCLOSURE</option>
              <option value="COMMUNICATION">COMMUNICATION</option>
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#94a3b8' }}>
            <span>Intervention:</span>
            <select
              value={interventionFilter}
              onChange={(e) => setInterventionFilter(e.target.value)}
              style={{ fontSize: '11px', padding: '4px 8px' }}
            >
              <option value="ALL">ALL INTERVENTIONS</option>
              <option value="STOP">STOP</option>
              <option value="WARN">WARN</option>
              <option value="ASK">ASK</option>
              <option value="ACT">ACT</option>
            </select>
          </div>
        </div>

        {/* Search */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', minWidth: '220px' }}>
          <Search size={13} color="#64748b" />
          <input
            type="text"
            placeholder="Search pairs, artifacts, actions..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ fontSize: '11px', padding: '4px 8px', width: '100%' }}
          />
        </div>
      </div>

      {/* Pairs Data Table */}
      <div className="table-container" style={{ maxHeight: '420px', overflowY: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th>Pair ID</th>
              <th>Base Artifact</th>
              <th>Category</th>
              <th>Intended Action</th>
              <th>Destination</th>
              <th>Expected Intervention</th>
              <th>Risk Type</th>
              <th>Ambiguity</th>
            </tr>
          </thead>
          <tbody>
            {filteredPairs.map((p) => (
              <tr key={p.pair_id}>
                <td style={{ fontFamily: 'monospace', fontWeight: 600, color: '#38bdf8' }}>
                  {p.pair_id}
                </td>
                <td style={{ fontFamily: 'monospace', color: '#94a3b8' }}>
                  {p.base_artifact_id}
                </td>
                <td style={{ color: '#cbd5e1', fontSize: '11px' }}>
                  {p.category}
                </td>
                <td style={{ fontFamily: 'monospace', color: '#f1f5f9' }}>
                  {p.intended_action}
                </td>
                <td style={{ color: '#94a3b8', fontSize: '11px' }}>
                  {p.destination}
                </td>
                <td>
                  {getInterventionBadge(p.expected_intervention)}
                </td>
                <td style={{ color: '#94a3b8', fontSize: '11px' }}>
                  {p.risk_type}
                </td>
                <td>
                  {p.is_ambiguous ? (
                    <span style={{ color: '#fbbf24', fontSize: '11px', fontWeight: 600 }}>Ambiguous</span>
                  ) : (
                    <span style={{ color: '#64748b', fontSize: '11px' }}>Clear</span>
                  )}
                </td>
              </tr>
            ))}
            {filteredPairs.length === 0 && (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                  No EARB pairs matched the filter criteria.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};
