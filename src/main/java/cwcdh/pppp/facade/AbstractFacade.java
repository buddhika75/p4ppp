/*
 * Author
 * Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 */
package cwcdh.pppp.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 * @param <T>
 */
public abstract class AbstractFacade<T> {

    private Class<T> entityClass;

    public void flush() {
        getEntityManager().flush();

    }

    public List<Object> findObjects(String jpql, Map<String, Object> parameters) {
        return findObjects(jpql, parameters, TemporalType.DATE);
    }

    public List<Object> findObjects(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object> qry = getEntityManager().createQuery(jpql, Object.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date pDate = (Date) pVal;
                qry.setParameter(pPara, pDate, TemporalType.DATE);
            } else {
                qry.setParameter(pPara, pVal);
            }
        }
        try {
            return qry.getResultList();
        } catch (Exception e) {
            return null;
        }
    }

    public List<Long> findLongList(String jpql, Map<String, Object> parameters) {
        return findLongList(jpql, parameters, TemporalType.DATE);
    }

    public List<Long> findLongList(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Long> qry = getEntityManager().createQuery(jpql, Long.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date pDate = (Date) pVal;
                qry.setParameter(pPara, pDate, TemporalType.DATE);
            } else {
                qry.setParameter(pPara, pVal);
            }
        }
        try {
            return qry.getResultList();
        } catch (Exception e) {
            return null;
        }
    }

    public List<T> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public T findFirstByJpql(String jpql, Map<String, Object> parameters) {
        try {
            TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
            Set s = parameters.entrySet();
            Iterator it = s.iterator();
            qry.setMaxResults(1);
            while (it.hasNext()) {
                Map.Entry m = (Map.Entry) it.next();
                String pPara = (String) m.getKey();
                if (m.getValue() instanceof Date) {
                    Date pVal = (Date) m.getValue();
                    qry.setParameter(pPara, pVal, TemporalType.DATE);
//                ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
                } else {
                    Object pVal = (Object) m.getValue();
                    qry.setParameter(pPara, pVal);
//                ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
                }
            }
            List<T> l = qry.getResultList();
            if (l != null && l.isEmpty() == false) {
                return l.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void create(T entity) {
        getEntityManager().persist(entity);
        //getEntityManager().flush();

    }

    public void refresh(T entity) {
        getEntityManager().refresh(entity);
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
    }

    public void editAndCommit(T entity) {
        getEntityManager().merge(entity);
        getEntityManager().getTransaction().commit();
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll(boolean withoutRetired) {
        javax.persistence.criteria.CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery<T> cq = cb.createQuery(entityClass);
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        ParameterExpression<String> p = cb.parameter(String.class);
        Predicate predicateRetired = cb.equal(rt.<Boolean>get("retired"), false);
        if (withoutRetired) {
            cq.where(predicateRetired);
        }
        return getEntityManager().createQuery(cq).getResultList();
    }

    public List<T> findAll() {
        javax.persistence.criteria.CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery<T> cq = cb.createQuery(entityClass);
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        return getEntityManager().createQuery(cq).getResultList();
    }

    
    public List<T> findByJpql(String jpql) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        return qry.getResultList();
    }

    public List<T> findByJpql(String jpql, int maxResults) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        qry.setMaxResults(maxResults);
        return qry.getResultList();
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
//        ////// //System.out.println("jpql = " + jpql);
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            String pPara = (String) m.getKey();
            if (m.getValue() instanceof Date) {
                Date pVal = (Date) m.getValue();
                qry.setParameter(pPara, pVal, TemporalType.DATE);
//                ////// //System.out.println("Parameter " + pPara + "\t Val =" + pVal);
            } else {
                Object pVal = (Object) m.getValue();
                qry.setParameter(pPara, pVal);
//                ////// //System.out.println("Parameter " + pPara + "\t Val =" + pVal);
            }
        }
        return qry.getResultList();
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }

        }
        return qry.getResultList();
    }

    public List<Object[]> findObjectsArrayByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        return qry.getResultList();
    }

    public List<Object> findObjectByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object> qry = getEntityManager().createQuery(jpql, Object.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        return qry.getResultList();
    }

    public Object findFirstObjectByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object> qry = getEntityManager().createQuery(jpql, Object.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }

        qry.setMaxResults(1);
        if (!qry.getResultList().isEmpty()) {
            return qry.getResultList().get(0);
        } else {
            return null;
        }
    }

    public Object[] findObjectListByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        return qry.getSingleResult();
    }

    public List<Date> findDateListByJpql(String jpql, Map<String, Object> parameters) {
        return findDateListByJpql(jpql, parameters, TemporalType.DATE);
    }

    public List<Date> findDateListByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Date> qry = getEntityManager().createQuery(jpql, Date.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        return qry.getResultList();
    }

    public List<Object> findObjectByJpql(String jpql) {
        TypedQuery<Object> qry = getEntityManager().createQuery(jpql, Object.class);
        return qry.getResultList();
    }

    public Object[] findAggregateModified(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        setParameterObjectList(qry, parameters, tt);

        try {
            Object[] obj = qry.getSingleResult();

            for (Object o : obj) {
                if (o == null) {
                    return null;
                }
            }

            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    public double findDoubleByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Double> qry = (TypedQuery<Double>) getEntityManager().createQuery(jpql);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
//                ////// //System.out.println("pval is a date");
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
//                ////// //System.out.println("p val is NOT a date");
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\t and Val\t " + pVal);
        }
        try {
            return (double) qry.getSingleResult();
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double findDoubleByJpql(String jpql) {
        TypedQuery<Double> qry = (TypedQuery<Double>) getEntityManager().createQuery(jpql);

        try {
            return (double) qry.getSingleResult();
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Date findDateByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Date> qry = (TypedQuery<Date>) getEntityManager().createQuery(jpql);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
//                ////// //System.out.println("pval is a date");
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
//                ////// //System.out.println("p val is NOT a date");
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\t and Val\t " + pVal);
        }
        try {
            return (Date) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public long findLongByJpql(String jpql, Map<String, Object> parameters) {
        return findLongByJpql(jpql, parameters, TemporalType.DATE);
    }

    public long findLongByJpql(String jpql, Map<String, Object> parameters, int maxResults) {
        TypedQuery<Long> qry = (TypedQuery<Long>) getEntityManager().createQuery(jpql);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, TemporalType.TIMESTAMP);
            } else {
                qry.setParameter(pPara, pVal);
            }
            qry.setMaxResults(maxResults);
        }
        try {
            return (long) qry.getSingleResult();
        } catch (Exception e) {
            //   //// //System.out.println("e = " + e);
            return 0l;
        }
    }

    public long findLongByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Long> qry = (TypedQuery<Long>) getEntityManager().createQuery(jpql);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
        }
        try {
            return (long) qry.getSingleResult();
        } catch (Exception e) {
            //   //// //System.out.println("e = " + e);
            return 0l;
        }
    }

    public double findDoubleByJpql(String jpql, Map<String, Object> parameters) {
        return findDoubleByJpql(jpql, parameters, TemporalType.DATE);
    }

    public Date findDateByJpql(String jpql, Map<String, Object> parameters) {
        return findDateByJpql(jpql, parameters, TemporalType.DATE);
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters, TemporalType tt, int maxRecords) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        qry.setMaxResults(maxRecords);
//        qry.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return qry.getResultList();
    }

    public List<T> findByJpqlWithoutCache(String jpql, Map<String, Object> parameters, TemporalType tt, int maxRecords) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        qry.setMaxResults(maxRecords);
        qry.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return qry.getResultList();
    }

    public List<T> findByJpqlWithoutCache(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
//        qry.setMaxResults(maxRecords);
        qry.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return qry.getResultList();
    }

    public List<T> findByJpqlWithoutCache(String jpql, Map<String, Object> parameters) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, TemporalType.DATE);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
        qry.setHint("javax.persistence.cache.storeMode", "REFRESH");
        return qry.getResultList();
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters, int maxRecords) {
        return AbstractFacade.this.findByJpql(jpql, parameters, TemporalType.DATE, maxRecords);
    }

    private void test(Class myClass, Object ob) {
    }

    public Long countByJpql(String sql) {
        return countByJpql(sql, null, TemporalType.DATE);
    }

    public Long countByJpql(String sql, Map parameters) {
        return countByJpql(sql, parameters, TemporalType.DATE);
    }

    public Long countByJpql(String sql, Map parameters, TemporalType tt) {
        TypedQuery<Long> q = getEntityManager().createQuery(sql, Long.class);
        if (parameters != null) {
            Set s = parameters.entrySet();
            Iterator it = s.iterator();
            while (it.hasNext()) {
                Map.Entry m = (Map.Entry) it.next();
                Object pVal = m.getValue();
                String pPara = (String) m.getKey();
                if (pVal instanceof Date) {
                    Date d = (Date) pVal;
                    q.setParameter(pPara, d, tt);
                } else {
                    q.setParameter(pPara, pVal);
                }
            }
        }
        Long l = q.getSingleResult();
        if (l == null) {
            l = 0l;
        }
        return l;
    }

    public double sumByJpql(String sql, Map parameters, TemporalType tt) {
        TypedQuery<Double> q = getEntityManager().createQuery(sql, Double.class);
        if (parameters != null) {
            Set s = parameters.entrySet();
            Iterator it = s.iterator();
            while (it.hasNext()) {
                Map.Entry m = (Map.Entry) it.next();
                Object pVal = m.getValue();
                String pPara = (String) m.getKey();
                if (pVal instanceof Date) {
                    Date d = (Date) pVal;
                    q.setParameter(pPara, d, tt);
                } else {
                    q.setParameter(pPara, pVal);
                }
            }
        }
        return q.getSingleResult();
    }

    public Double sumByJpql(String sql) {
        Query q = getEntityManager().createQuery(sql);
        return (Double) q.getSingleResult();
    }

    public T findFirstByJpql(String jpql) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        qry.setMaxResults(1);
        try {
            return qry.getResultList().get(0);
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return null;
        }
    }

    public T findFirstByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<T> qry = getEntityManager().createQuery(jpql, entityClass);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        qry.setMaxResults(1);
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
            //    ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }

        if (!qry.getResultList().isEmpty()) {
            return qry.getResultList().get(0);
        } else {
            return null;
        }
    }

    public <U> List<T> testMethod(U[] a, Collection<U> all) {
        List<T> myList = new ArrayList<T>();
        return myList;
    }

    public <U> List<T> findAll(String fieldName, int searchID, boolean withoutRetired) {

//        final long userId,
//    final long contactNumber){
//
//    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//    final CriteriaQuery<TaUser> query = cb.createQuery(TaUser.class);
//    final Root<TaUser> root = query.from(TaUser.class);
//    query
//        .where(cb.and(
//            cb.equal(root.get("userId"), userId),
//            cb.equal(root.get("taContact").get("contactNumber"), contactNumber)
//        ));
//    return entityManager.createQuery(query).getSingleResult();
        javax.persistence.criteria.CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery<T> cq = cb.createQuery(entityClass);
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);

        if (withoutRetired) {
            cq.where(cb.and(cb.equal(rt.get("retired"), false)),
                    (cb.equal(rt.get(fieldName).get("id"), searchID)));
        } else {
            cq.where(cb.equal(rt.get("retired"), false));
        }

        return getEntityManager().createQuery(cq).getResultList();
    }

    public int count() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        return ((Long) q.getSingleResult()).intValue();
    }

    public Double findAggregateDbl(String strJQL) {
        Query q = getEntityManager().createQuery(strJQL);
        Double temd;
        try {
            temd = (Double) q.getSingleResult();
            if (temd == null) {
                temd = 0.0;
            }
        } catch (Exception e) {
            ////// //System.out.println(e.getMessage());
            temd = 0.0;
        }
        return temd;
    }

    public Long findAggregateLong(String strJQL) {
        Query q = getEntityManager().createQuery(strJQL);
        Long temd;
        try {
            temd = (Long) q.getSingleResult();
            if (temd == null) {
                temd = 0L;
            }
        } catch (Exception e) {
            temd = 0L;
        }
        return temd;
    }

    public Long findLongByJpql(String strJQL) {
        Query q = getEntityManager().createQuery(strJQL);
        try {
            return (Long) q.getSingleResult();
        } catch (Exception e) {
            return 0l;
        }
    }

    public List<String> findString(String strJQL) {
        Query q = getEntityManager().createQuery(strJQL);
        try {
            return q.getResultList();
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return null;
        }
    }

    public List<String> findString(String strJQL, Map map) {
        return findString(strJQL, map, TemporalType.DATE);
    }

    public List<String> findString(String strJQL, Map map, TemporalType tt, int noOfRows) {
        Query q = getEntityManager().createQuery(strJQL);
        Set s = map.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            String pPara = (String) m.getKey();
            if (m.getValue() instanceof Date) {
                Date pVal = (Date) m.getValue();
                q.setParameter(pPara, pVal, tt);
            } else {
                q.setParameter(pPara, m.getValue());
            }
        }
        if (noOfRows != 0) {
            q.setMaxResults(noOfRows);
        }
        try {
            return q.getResultList();
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return null;
        }
    }

    public List<String> findString(String strJQL, Map map, TemporalType tt) {
        return findString(strJQL, map, tt, 0);
    }

    public List<Object[]> findAggregates(String jpql, Map<String, Object> parameters) {
        return findAggregates(jpql, parameters, TemporalType.DATE);
    }

    public List<Object[]> findAggregates(String jpql) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        try {
            return qry.getResultList();
        } catch (Exception e) {
            //   //// //System.out.println("e = " + e.getMessage());
            return null;
        }
    }

    public List<Object[]> findAggregates(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date pDate = (Date) pVal;
                // qry.setParameter(pPara, pDate, TemporalType.DATE);
                qry.setParameter(pPara, pDate, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
        }
        try {
            return qry.getResultList();
        } catch (Exception e) {
            return null;
        }
    }

    private void setParameterObjectList(TypedQuery<Object[]> qry, Map<String, Object> parameters, TemporalType temporalType) {
        if (parameters == null) {
            return;
        }

        Set s = parameters.entrySet();
        Iterator it = s.iterator();

        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            String pPara = (String) m.getKey();
            if (m.getValue() instanceof Date) {
                Date pVal = (Date) m.getValue();
                qry.setParameter(pPara, pVal, temporalType);
            } else {
                Object pVal = (Object) m.getValue();
                qry.setParameter(pPara, pVal);
            }
        }
    }

    public Object[] findAggregat(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        setParameterObjectList(qry, parameters, tt);

        try {
            Object[] obj = qry.getSingleResult();

            for (Object o : obj) {
                if (o == null) {
                    return null;
                }
            }

            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    public Object[] findAggregate(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date pDate = (Date) pVal;
                // qry.setParameter(pPara, pDate, TemporalType.DATE);
                qry.setParameter(pPara, pDate, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
        }
        try {
            return qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public Object[] findSingleAggregate(String jpql, Map<String, Object> parameters) {
//        ////// //System.out.println("find aggregates 2" );
        return findSingleAggregate(jpql, parameters, TemporalType.DATE);
    }

    public Object[] findSingleAggregate(String jpql, Map<String, Object> parameters, TemporalType tt) {
//        ////// //System.out.println("find aggregates 3");
        TypedQuery<Object[]> qry = getEntityManager().createQuery(jpql, Object[].class);
//        ////// //System.out.println("2");
        Set s = parameters.entrySet();
//        ////// //System.out.println("m " + parameters);
//        ////// //System.out.println("s = " + s);
//        ////// //System.out.println("3");
        Iterator it = s.iterator();
//        ////// //System.out.println("4");
        while (it.hasNext()) {
//            ////// //System.out.println("5");
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date pDate = (Date) pVal;
                qry.setParameter(pPara, pDate, TemporalType.DATE);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }
//        ////// //System.out.println("6");
        try {
            return qry.getSingleResult();
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return null;
        }
    }

    public Double findAggregateDbl(String jpql, Map<String, Date> parameters) {
        Query qry = getEntityManager().createQuery(jpql);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();

        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Date pVal = (Date) m.getValue();
            String pPara = (String) m.getKey();
            qry.setParameter(pPara, pVal, TemporalType.DATE);
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }

        try {
            return (Double) qry.getSingleResult();
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return 0.0;
        }
    }

    public Long findAggregateLong(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<Long> qry = getEntityManager().createQuery(jpql, Long.class);
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            if (pVal instanceof Date) {
                Date d = (Date) pVal;
                qry.setParameter(pPara, d, tt);
            } else {
                qry.setParameter(pPara, pVal);
            }
//            ////// //System.out.println("Parameter " + pPara + "\tVal" + pVal);
        }

        try {
            return (Long) qry.getSingleResult();
        } catch (Exception e) {
//            ////// //System.out.println(e.getMessage());
            return 0L;
        }
    }
}
